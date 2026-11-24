# 06-5. 업무일지 연결 업무 저장 (Sequence)

API: PUT /api/me/worklogs/{date}/tasks/{taskId}

참여자: 사용자, WorklogTaskService, WBS 권한·진척 서비스, DB.

```mermaid
sequenceDiagram
    participant U as 사용자
    participant W as 업무일지 서비스
    participant T as 업무 서비스
    participant D as DB
    U->>W: 업무별 기록과 버전
    W->>T: 프로젝트·담당자 권한 확인
    T->>D: 프로젝트 행 잠금
    W->>D: 사용자 행 잠금, 기록 조회
    alt 동일 기록 재전송
        W-->>U: 기존 기록
    else 입력 변경
        W->>T: 업무 진척 또는 완료 반영
        T->>D: 업무·상위 진척·이력 저장
        W->>D: 연결 기록 저장, 커밋
        W-->>U: 저장 결과
    end
```

핵심 포인트: 권한 검사 후 잠금, 프로젝트→사용자 고정 순서, 단일 트랜잭션. 실패 시 전체 롤백. PM 조회는 프로젝트에 연결된 업무별 본문만 대상으로 하며 날짜별 개인 본문을 포함하지 않는다.
