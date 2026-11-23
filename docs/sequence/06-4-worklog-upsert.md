# 06-4. 업무일지 저장

## API

`PUT /api/me/worklogs/{logDate}` — FR-PSN-03, SCRUM-121. 본인 날짜별 Markdown 노트를 생성·교체한다. 업무 연결·진척률 반영은 이 흐름에 아직 없다.

## 참여자

사용자 → 인증 필터 → WorklogController → WorklogService(트랜잭션) → EntityManager(users) → WorklogRepository(worklogs) → DB → GlobalExceptionHandler(오류).

## 핵심 포인트

- 로그인 검증 뒤 서버 인증 주체의 userId만 사용한다. content 길이 검증을 통과한 요청만 서비스로 보낸다.
- users 행을 먼저 `PESSIMISTIC_WRITE`로 잠그고 worklogs를 조회한다. 잠금은 트랜잭션 종료까지 유지한다.
- 같은 사용자·날짜의 동시 첫 저장은 사용자 행 잠금으로 직렬화되고, 유일 제약이 최종 중복을 막는다.
- 같은 내용이면 dirty checking이 UPDATE를 생략한다. flush·refresh 후 DB 정밀도의 시각을 응답한다.
- 직접 JPA 잠금 timeout/획득 실패는 CustomException으로 변환되어 409를 반환한다. 트랜잭션은 롤백되고 GlobalExceptionHandler의 예상 밖 500·에러 로그 경로로 내려가지 않는다.

## 판정 순서

```mermaid
sequenceDiagram
    actor U as 사용자
    participant C as Controller
    participant S as WorklogService
    participant D as DB
    participant H as GlobalExceptionHandler
    U->>C: 인증된 PUT 날짜와 본문
    C->>C: 날짜 형식 및 본문 10000자 검증
    C->>S: upsert(인증 userId, 날짜, 내용)
    S->>D: 트랜잭션 시작, users 행 쓰기 잠금
    alt 잠금 실패 또는 timeout
        S->>S: CONCURRENT_MODIFICATION으로 변환
        S->>D: 롤백
        S-->>H: CustomException
        H-->>U: 409, 원문과 DB 상세 비노출
    else 소유자 없음
        S->>D: 롤백
        S-->>H: USER_NOT_FOUND
        H-->>U: 404
    else 잠금 성공
        S->>D: userId와 날짜로 worklogs 조회
        S->>D: 없으면 INSERT, 있으면 내용 교체
        S->>D: flush 및 refresh
        D-->>S: 저장 결과
        S->>D: 커밋, 사용자 행 잠금 해제
        S-->>C: WorklogResult
        C-->>U: 200 업무일지
    end
```

두 번째 동시 요청은 첫 요청의 커밋 뒤 잠금을 얻고 기존 worklog를 읽는다. 동일 내용 재전송이면 행 추가와 수정 시각 변경이 없다. 서로 다른 내용의 동시 저장은 직렬화된 순서대로 마지막 쓰기가 반영되며, 사용자에게 낙관적 version 비교를 제공하는 기능은 현재 계약에 없다.
