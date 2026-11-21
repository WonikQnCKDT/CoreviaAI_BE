# SCRUM-111 구현 초안 검수 기록

> 상태: Draft · 담당자: 미정 · 마지막 검토일: 2026-11-22
> 적용 범위: FR-PSN-01 개인 TODO 등록·상세·수정·배정 WBS 업무 통합 조회
> 기준 원격 develop: `6d39260dcb17971e5a41d6a8be3eb315a1f06617` (2026-11-21 확인)

## 1. 요구사항 확인과 미완료 범위

Jira SCRUM-111 및 상위 SCRUM-85, 요구사항 명세서 최종안(전사관리) FR-PSN-01·3.3, Confluence ERD 06·00 공통 규칙, API 공통 규약을 읽었다. 프로젝트 root에 README.md는 없어 백엔드 README를 읽었다. 프로젝트 공통·Backend 개발 원칙을 적용했다.

현재 초안에는 개인 등록·상세·전체 값 수정·완료/재개, 현재 배정 WBS의 실시간 통합 조회가 있다. 다음 항목은 결정을 기다리므로 SCRUM-111 완료로 간주하지 않는다.

- 원격 develop에 승인 요청 원천(산출물 검수·게이트 승인 워크플로)이 없다. 원천 구현까지 이번 범위에 포함할지, 후속 연동할지 사용자 확인 대기.
- ERD의 개인 TODO 삭제 정책이 미정. DELETE API·삭제 감사 로그·삭제 이후 요청 멱등 키 보존 미구현.
- 마감 임박 기간 기본값이 명세서에 없다. 초안은 dueSoonDays 필수 query로 받고 최종 기간·기본값 확인 대기.

## 2. 보안 브리프

| 항목 | 설계·검증 |
| --- | --- |
| 신뢰 경계 | HTTP JSON·path·query → 요청 검증 → 서비스 command → owner-scoped 저장소. 인증 주체는 기존 LoginUser principal, 소유자는 body에서 받지 않음 |
| 개인 데이터 가정 | TODO 제목은 개인 업무 원문. 요청 DTO toString은 원문을 가리고, 본문·제목·해시를 일반 로그·감사 로그에 남기지 않음 |
| 개인 권한 | 상세·수정 SQL에 id와 user_id 동시 제한. 다른 사용자·시스템 관리자도 타인 개인 TODO 404 |
| 프로젝트 권한 | 현재 담당자 + 현재 멤버, USER/EXECUTIVE만 업무 조회. EXECUTIVE는 기존 공개·부서 범위 적용. SYSTEM_ADMIN 개인 TODO는 허용하되 프로젝트 업무는 제외 |
| 인가 회수 | 매 페이지마다 DB의 현재 역할·멤버·소속을 검사. 기존 cursor도 권한을 보존하지 않음 |
| 입력 | 제목 필수·200자, UUID 요청 키 필수, DATE 허용 범위 1~9999년, 양수 ID, size1~100·days1~365, 커서 길이·스코프·형식 검증 |
| 주입 공격 | SQL 구조 고정, NamedParameterJdbcTemplate 바인딩 사용. 사용자 입력을 SQL 문자열로 조립하지 않음 |
| 동시성 | 생성은 users 행 → 개인 TODO 순서 잠금과 unique(user,requestId), 수정은 본인 행 잠금. 동일 생성 요청 동시 재전송 통합 테스트 |
| 실패 처리 | 표준 ErrorCode/ApiResponse, 없는 항목·타인 항목 동일 404, 같은 요청 키 다른 내용409, 잠금 실패409(기존 예외 처리) |
| 감사 | 현재 구현에는 삭제·권한·설정 변경 이벤트 없음. 삭제 도입 시 원문 없이 삭제 감사 로그를 같은 트랜잭션에 남기는 후속 설계 필요 |

보안 설계·검수용 Agensi security-first, code-reviewer, java-best-practice-checker는 root가 매번 호출했으나 재인증 오류(UNAUTHORIZED)로 불러오지 못했다. 스킬을 파일로 저장하지 않았으며, 공통 Git Workflow의 AI 생성 코드 리뷰 6단계와 독립 에이전트 검수로 대체했다.

## 3. AI 코드 리뷰 순서

1. 의도: 개인만 저장하고 프로젝트 업무를 실시간 합쳐 복제·정리 작업 없이 배정 변경을 다음 조회에 반영한다. 단일 bounded UNION 조회로 N+1을 만들지 않는다.
2. 의존성: Java 17·기존 Spring/JPA/JDBC만 사용하고 새 dependency·패키지를 추가하지 않았다.
3. 경계: 날짜 없음·최솟값·최댓값, 제목 빈 값·200/201자, 페이지0/1/100/101, 날짜 임박 경계·KST 자정, 같은 숫자 ID의 다른 source, 동시 같은 요청 키를 테스트한다.
4. 숨은 가정: 시스템 관리자 승격·프로젝트 멤버 제외 뒤에도 assignee가 남을 수 있어 현재 권한을 SQL에 다시 제한했다. 커서의 정렬 날짜 고정과 실시간 행·권한의 차이를 API 문서에 기록했다.
5. 실패: 요청 키 충돌409, 타인 개인404, 잘못된 커서400, 입력 오류400, 미인증401, 완료 반복·재개, 범위 밖 역할·부서의 프로젝트 업무 제외를 검증한다.
6. 자동 검증: Java 컴파일·단위·통합 테스트·패키징 증거는 아래 명령과 결과로 기록한다. 기존 미구축 정적·공급망 gate까지 통과했다고 표현하지 않는다.

## 4. 자동 수정한 검수 지적

- 개인 TODO만 owner 제한하면 통합 WBS 경로가 프로젝트 권한 우회가 될 수 있다는 독립 검수 지적 → 현재 멤버·시스템 역할·경영진 부서 범위를 통합 SQL에서 제한하고 기존 커서의 권한 회수 테스트 추가.
- PowerShell의 기본 파이프 인코딩으로 신규 ErrorCode 한국어가 `?`로 저장됨 → UTF-8 apply_patch로 복구하고 diff 확인.
- 경계 테스트가 dueSoonDays query를 중복으로 넣어 잘못된 검증이 될 수 있음 → 요청당 단일 query 키로 수정.
- 기존 MutableClock.withZone이 zone을 무시해 날짜 경계 검증에서 UTC로 남음 → Instant에서 Asia/Seoul 날짜로 명시적 변환.
- 최초 CTE UNION 구현은 통합 테스트에서 완료 필터·사용자 역할 파라미터를 바꾼 뒤 기대한 결과를 주지 않음 → 같은 권한·필터를 유지한 derived-table UNION으로 바꾸고 13개 통합 테스트 재통과. 외부 DBMS의 일반 결함이라고 단정하지 않는다.
- DB TIMESTAMP(6)보다 정밀한 Clock의 최초 생성·수정/완료 시각과 DB 재조회 시각이 달라 멱등 응답이 달라질 수 있음 → INSERT·변경 UPDATE의 FLUSH 후 DB refresh로 저장 정밀도를 반영하고, 나노초 Clock의 생성 재전송·완료 PATCH 재전송·GET 일치 테스트 추가.
- PostgreSQL VARCHAR가 허용하지 않는 NUL 등 제어 문자가 제목에 들어오면 DB 오류가 될 수 있음 → 등록·수정 요청 제목에 제어 문자 금지 검증, NUL·줄바꿈 입력의 400 테스트 추가.
- V13 선적용 시 후속 예약 V8~V12가 Flyway outOfOrder=false에서 적용 불가 → SQL·ERD·README에 V8~V12 통합·승인 후 순서 배포, V13 단독 선적용 금지 기록.

## 5. 검증 명령과 환경 제약

환경: Windows, JDK17, 저장소 Gradle Wrapper9.8.2. 사용자 홈에 쓰지 않고 작업 공간 `.codex-work/.gradle-home`에 기존 캐시 사본을 사용했다.

표준 명령:

```powershell
.\gradlew.bat --offline --no-daemon -g ../.gradle-home compileJava
.\gradlew.bat --offline --no-daemon -g ../.gradle-home test integrationTest --tests '*PersonalTodo*' --tests '*TodoDatePolicy*' --tests '*TodoCursor*'
```

컴파일은 성공했다. 표준 test는 `org.jacoco:org.jacoco.agent:0.8.15`의 오프라인 캐시가 없어 의존성 확인 단계에서 실패했다. 네트워크가 제한돼 다운로드하지 않았다. 커버리지나 표준 check 통과로 해석하지 않는다.

테스트 동작 자체를 확인하는 대체 명령(저장소 build.gradle·CI 변경 없음):

```powershell
.\gradlew.bat --offline --no-daemon -g ../.gradle-home -I ../tests-without-jacoco.gradle test integrationTest
.\gradlew.bat --offline --no-daemon -g ../.gradle-home -I ../tests-without-jacoco.gradle bootJar
```

작업 공간 전용 init 파일의 최종 내용:

```groovy
gradle.projectsEvaluated {
    allprojects { project ->
        project.tasks.withType(Test).each { testTask ->
            testTask.extensions.getByType(org.gradle.testing.jacoco.plugins.JacocoTaskExtension).enabled = false
        }
    }
}
```

초기 대체 init 2회는 Jacoco extension 설정 시점이 빨라 실패했고, 위 projectsEvaluated 방식으로 수정했다. 위 대체 명령은 JaCoCo instrumentation을 끄므로 커버리지 증거를 생성하지 않는다. 테스트 결과 최종 수치·빌드 결과는 인계 직전 아래에 추가한다.

### 최종 실행 결과 (2026-11-22)

| 소스 범위 | 명령·증거 | 결과 |
| --- | --- | --- |
| DB 시각 정밀도·제어 문자 최종 보완 전, CTE 제거·KST 변환 반영 상태 | 대체 init으로 `test integrationTest bootJar`, 2026-11-21 23:59:46 KST 종료 | 단위302·전체 통합137, 실패·오류0. bootJar 성공, 3분04초 |
| **최종 소스**: POST·PATCH 저장 후 DB refresh, 나노초 응답 일치 2개 테스트·제어 문자 검증 포함 | `gradlew.bat --offline --no-daemon -g ../.gradle-home -I ../tests-without-jacoco.gradle test integrationTest --tests '*PersonalTodoApiIntegrationTest' bootJar`, 2026-11-22 00:04:33 KST 종료 | 단위302·개인 TODO 통합15, 실패·오류0. bootJar 성공, 1분29초 |
| 최종 변경 diff·보완 ERD | `git diff --check`, Python XML 파싱 | 공백 오류 없음·drawio XML 문법 정상 |

최종 소스에서 다른 도메인 통합124개는 다시 실행하지 않았다. 직전 전체137개 성공과 최종 TODO15개 성공을 구분하며, 최종 전체139개를 통과했다고 합산해 주장하지 않는다. 최종 두 보완의 영향 경로(생성·완료 재전송·GET·입력 거부)는 최종15개 통합에 포함된다. 표준 check·커버리지·정적/공급망 gate 전체 통과 증거는 아니다.

## 6. 기존 미구축 gate의 한계와 후속 조치

| 항목 | 기록 |
| --- | --- |
| 미충족 | 프로젝트 formatter·lint/static-analysis·dependency lock·dependency/secret security scan 구축 및 커버리지 실행 증거 |
| 사유 | 기준 develop에 도구·lockfile이 없고 JaCoCo agent0.8.15도 로컬 오프라인 캐시에 없음. 이번 기능 범위에서 의존성·CI 정책을 임의 도입하지 않음 |
| 영향·위험 | 자동 형식·정적·공급망·secret·커버리지 결과 미확보. 런타임 테스트 성공은 이 검증을 대체하지 않음 |
| 검토 대안 | 수동 diff·신규 의존성 없음 확인·독립 코드 리뷰·Java17 컴파일·단위·통합 실행. 정식 gate는 팀이 도입 필요 |
| 승인 주체 | 미정. 본 기록은 규칙 예외 승인이나 merge 승인 자체가 아님 |
| 기록 위치 | 본 문서, root 최종 보고·PR 초안에서 참조 |
| 재검토 | merge 전 정식 네트워크·CI 환경에서 표준 check/coverage 및 미구축 gate 보완 |

## 7. 문서·배포·인계

- API 초안: `docs/api/06-personal-todos-draft.md`.
- ERD 초안: `docs/erd/06-personal-todos-draft.md`, 보완 원본 `06-personal-todos.drawio`. 기존 전체 도메인 그림을 대체하지 않는다. 원본 첨부 다운로드·PNG export·Confluence 이미지 동기화 미완료.
- 시퀀스 초안: `docs/sequence/06-personal-todos-draft.md`. 단순 CRUD 설명 대신 생성 멱등 키 처리·잠금(users → personal_todos)·통합 조회 인가 순서를 남겼다. 승인 원천 연동의 참여자·보안 판정 흐름은 범위 결정 후 작성 대상이다.
- V13은 현재 V1~V7 테스트 기준에서 실행 가능하나 운영에는 예약 V8~V12 통합·승인 후 순서 적용해야 한다. V13 단독 운영 선배포 금지, outOfOrder 임의 활성화 금지.
- 원격 push·원격 브랜치 생성·Jira 상태 변경은 하지 않는다. 공개 문서·Jira 댓글은 root가 실제 완료·미완료 범위를 구분해 기록한다.
