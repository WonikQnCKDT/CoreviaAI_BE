# 06. 개인 업무 (API) — SCRUM-111 구현(PR 전)

> 상태: Draft · 담당자: 미정 · 마지막 검토일: 2026-11-22
> 기준: FR-PSN-01, 요구사항 명세서 3.3 PSN-01·02, ERD 06
> Confluence 정식 게시 전 검토용. 사용자 결정: 승인 요청 원천은 후속 연결, 개인 TODO는 소프트 삭제, 마감 임박 기본3일.

- 관련: [SCRUM-111](https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-111), [전사관리 요구사항](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909), [06. 개인 업무 ERD](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/20414469).
- 공식 API 명세서 템플릿과 [00. 공통 규약 (API)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/12877846)을 따른다.

## 요약

| Method | Path | 설명 | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/api/me/todos` | 개인 TODO·배정 WBS 업무 통합 목록 | 로그인 전원, 본인 | 구현(PR 전) |
| POST | `/api/me/todos` | 개인 TODO 등록 | 로그인 전원, 본인 | 구현(PR 전) |
| GET | `/api/me/todos/{todoId}` | 개인 TODO 상세 | 로그인 전원, 본인 | 구현(PR 전) |
| PATCH | `/api/me/todos/{todoId}` | 개인 TODO 제목·마감·완료 수정 | 로그인 전원, 본인 | 구현(PR 전) |
| DELETE | `/api/me/todos/{todoId}` | 개인 TODO 소프트 삭제 | 로그인 전원, 본인 | 구현(PR 전) |

## 공통 사항

- 응답 형식·쿠키 인증·공통 오류는 Confluence `00. 공통 규약 (API)`를 따른다. 클라이언트가 소유자 ID를 지정하는 경로·본문은 없다. 상세·변경은 DB에서 `(id, user_id)`로 함께 제한한다. 다른 사람의 항목과 없는 항목은 모두 404 `TODO_NOT_FOUND`다. 시스템 관리자도 다른 사람의 개인 TODO를 볼 수 없다.
- 개인 할 일만 `personal_todos`에 저장한다. 배정 업무는 매 조회마다 `wbs_tasks`에서 합친다. 배정·배정 해제·완료가 커밋되면 다음 조회에 바로 반영된다. 폴링 지연이나 복제 작업이 없다. UI는 1분 이내 재조회해야 1분 표시 인수 기준을 충족한다.
- 프로젝트 업무는 현재 담당자이면서 현재 프로젝트 멤버여야 한다. 시스템 관리자에게는 업무를 반환하지 않는다. 경영진은 기존 프로젝트 공개 정책대로 전사 공개 또는 소속·하위 부서 주관 프로젝트만 반환한다. 각 페이지마다 최신 역할·멤버·부서 범위를 다시 검사한다. 개인 TODO는 모든 시스템 역할의 본인 항목을 허용한다.
- 완료된 배정 업무는 항상 제외한다. 완료 개인 TODO는 `done=true` 조회로만 나온다. 배정 업무의 상태·완료 변경은 해당 업무 API의 책임이며 개인 TODO 변경 경로로 수정할 수 없다. `source`와 `id`가 항목의 복합 식별자다. `TASK` ID를 개인 상세·변경 API에 보내도 개인 TODO 외 데이터는 접근하지 않는다.
- 오늘은 주입된 Clock의 `Asia/Seoul` 달력 날짜다. timestamp는 UTC Instant, 마감일은 DATE다. 정렬은 오늘(0) → 마감 임박(1) → 지연(2) → 이후(3) → 마감 없음(4), 같은 그룹은 마감일 오름차순 → PERSONAL(0)/TASK(1) → ID 오름차순이다. 지연은 `overdue=true`로 별도 제공한다.
- 마감 임박 기본값은 사용자 확정에 따라3일이다. `dueSoonDays` 생략 시3, 명시하면1~365 범위를 허용한다. 오늘은 별도 그룹이며 다음 날부터 `오늘 + dueSoonDays`까지 양끝 포함(D+1~D+3 기본)이다.
- 개인 TODO는 V14 `deleted_at`으로 소프트 삭제한다. 활성·완료 개인 목록과 단건 조회·수정은 삭제행을 제외하고, 기존 cursor도 삭제행을 반환하지 않는다. 삭제된 본인 항목의 DELETE 재요청은204, 타인·없는 항목은404다. WBS 업무는 이 API로 삭제하지 않는다.
- 커서는 사용자·done·임박 일수·정렬 날짜·마지막 복합 키를 포함한 불투명 Base64URL 값이다. 형식·다른 사용자·필터가 맞지 않으면 400 `INVALID_CURSOR`. 커서를 이어가는 동안 정렬 날짜와 `overdue` 판정 날짜를 고정해 KST 자정 경계의 중복·누락을 줄인다. 매 행의 현재 상태·권한은 실시간이다. 새 날 정렬을 보려면 커서를 버리고 첫 페이지부터 조회한다. 항목 수정·배정 변경 중 전체 스냅샷 일관성은 보장하지 않는다.
- 개인 수정은 행 잠금으로 순차 처리한다. 마지막 수정 요청이 이기며, 같은 값 재전송은 수정 시각도 바꾸지 않는다. 완료 재전송은 최초 `doneAt`을 유지하고 완료 해제는 null로 만든다.
- 이 초안에 새 외부 연동·캐시·자동 재시도·추가 라이브러리는 없다. 별도 TODO rate limit은 없고 기존 인증·세션 정책을 따른다.

## enum 값

| 이름 | 값 |
| --- | --- |
| `source` (응답 항목 구분) | `PERSONAL`, `TASK` |
| `taskStatus` (`WbsTaskStatus`, TASK만) | `TODO`, `IN_PROGRESS`, `REVIEW`, `DONE`. 완료 업무는 통합 목록에서 제외 |
| `urgency` (정렬 그룹) | `0` 오늘, `1` 임박, `2` 지연, `3` 이후, `4` 마감 없음 |

## GET /api/me/todos — 통합 목록

| Query | 필수 | 제약 |
| --- | --- | --- |
| `cursor` | N | 첫 요청은 생략. 이전 `nextCursor`를 변경 없이 전달, 최대 256자 |
| `size` | N | 기본 20, 1~100 |
| `done` | N | 기본 false. true는 완료된 개인 항목만 |
| `dueSoonDays` | N | 기본3, 1~365. 기본은 오늘 제외 D+1~D+3 |

200 성공 응답의 `response`: `{ "items": [TodoItemResponse...], "nextCursor": "..." 또는 null, "hasNext": true 또는 false }`.

| 항목 필드 | 타입 | 의미 |
| --- | --- | --- |
| `id`, `source` | number, string | 소스 내 ID, `PERSONAL` 또는 `TASK` |
| `title`, `dueDate` | string, date 또는 null | 본문과 마감일 |
| `done`, `doneAt` | boolean, UTC timestamp 또는 null | 개인 완료 상태·시각. 배정 업무는 false/null |
| `projectId`, `taskStatus` | number 또는 null, string 또는 null | TASK의 프로젝트와 고정 업무 코드. PERSONAL은 null |
| `urgency`, `overdue` | number, boolean | 정렬 그룹 0~4와 지연 표시용 값. UI 표시 문구는 다국어 라벨 사용 |

오류: 400 파라미터 형식·범위 또는 `INVALID_CURSOR`, 401 미인증·만료, 403 비밀번호 변경 강제(공통 인증 정책).

## POST /api/me/todos — 개인 할 일 등록

| Body | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `requestId` | UUID | Y | 생성 요청의 멱등 키. 재전송할 때 같은 UUID 사용 |
| `title` | string | Y | 공백만 불가, 입력 200자 이하, 제어 문자(NUL·줄바꿈·탭 등) 금지. 양끝 공백 제거 |
| `dueDate` | date 또는 null | N | `0001-01-01`~`9999-12-31`, ISO 날짜. 과거 날짜 허용 |

201 성공 `response`: `PersonalTodoResponse`. 필드: `id`, `title`, `dueDate`, `done=false`, `doneAt=null`, `createdAt`, `updatedAt`(UTC).

멱등성: `(user_id, request_id)` 유일 키. 삭제되지 않은 행의 같은 키·정규화한 제목·마감일이면 최초 생성 응답을 그대로 반환하고 새 행을 만들지 않는다. 이후 제목 변경·완료 후에도 불변 `request_hash`로 최초 입력을 대조한다. 같은 키·다른 입력은409 `TODO_REQUEST_CONFLICT`다. **소프트 삭제행도 요청 키·해시·유일 제약을 보존하며, 같은 최초 입력 재전송은409 `TODO_DELETED`로 거부한다. 삭제행을 부활시키거나 새 행을 만들지 않는다.** 새 할 일은 새 UUID로 생성한다. 제목 중복 자체는 허용한다. 소유자 행 잠금 → 키·해시·삭제 여부 확인 → 생성 순서와 DB unique로 동시 재전송을 제한한다. 삭제행·멱등 키의 보존 기간·물리 purge·복구 기능은 미정이며 자동으로 수행하지 않는다.

오류: 400 입력 검증·`INVALID_TODO_DATE`, 401 미인증, 409 `TODO_REQUEST_CONFLICT`·`TODO_DELETED`·`CONCURRENT_MODIFICATION`, 404 `USER_NOT_FOUND`(비정상 인증 주체), 기타 공통 인증 오류.

## GET /api/me/todos/{todoId} — 개인 상세

- Path: `todoId` 양수. 200 `response`: PersonalTodoResponse (`id`, `title`, `dueDate`, `done`, `doneAt`, `createdAt`, `updatedAt`).
- 오류: 400 양수 위반, 401 미인증, 404 `TODO_NOT_FOUND`(없거나 본인 항목 아님). 프로젝트 업무 상세 조회 경로가 아니다.

## PATCH /api/me/todos/{todoId} — 개인 수정·완료

- Path: `todoId` 양수. Body: `title` 필수(등록과 동일), `dueDate` 선택(생략·null이면 마감 해제), `done` 필수 boolean. 보낸 값으로 덮어쓴다.
- 200 `response`: PersonalTodoResponse. 완료 전환 시 주입 Clock의 UTC 시각을 `doneAt`에 저장한다. 취소 시 null.
- 오류: 400 입력 검증·`INVALID_TODO_DATE`, 401 미인증, 404 `TODO_NOT_FOUND`, 409 `CONCURRENT_MODIFICATION`(잠금 획득 실패). 같은 값 재전송은 변경 없음.

## DELETE /api/me/todos/{todoId} — 개인 소프트 삭제

- 인증: 로그인 전원, 본인 개인 항목만. Path `todoId` 양수. Body·별도 헤더 없음.
- 성공:204, 본문 없음. 최초 요청에서 주입 Clock의 UTC 시각을 `deleted_at`에 기록하고 삭제 감사 로그를 같은 트랜잭션에 추가한다. 삭제와 감사 중 어느 하나가 실패하면 모두 롤백한다.
- 멱등성: 삭제된 본인 행에도204, 최초 삭제 시각 유지·감사 로그 중복 없음. 본인행 잠금으로 동시 DELETE도 감사1건만 남긴다. 타인·없는 ID는404 `TODO_NOT_FOUND`로 동일하게 처리한다.
- 감사: `PERSONAL_TODO_DELETED` / `PERSONAL_TODO`, target ID와 행위자·기존 공통 IP, details는 ownerUserId만. 제목·본문·requestId·해시를 기록하지 않는다. 개인 완료 여부·완료 시각과 생성 키는 그대로 보존한다.
- 삭제 후 GET/PATCH는404 `TODO_NOT_FOUND`, 개인 목록의 done false/true 및 기존cursor에서 제외된다. 복구·물리 삭제 API는 없다.
- 오류:400 양수 위반, 401 미인증, 404 `TODO_NOT_FOUND`, 409 `CONCURRENT_MODIFICATION`(잠금 실패), 500 감사 기록 등 예기치 않은 실패(삭제도 롤백), 기타 공통 인증 정책.

## 승인된 후속·운영 미결

1. 승인 요청 원천(산출물·게이트 승인 워크플로)은 원격 develop에 없다. **사용자가 해당 기능 구현 후 연결하는 후속 범위를 승인했다.** 현재 구현하지 않은 승인 원천을 완료로 표현하지 않는다.
2. FE 화면은 이 BE 저장소에 없으며, 화면에서1분 이내 재조회해야 업무 배정 표시 인수 기준을 완성한다. BE는 커밋 후 다음 조회 즉시 반영한다.
3. 소프트 삭제행·멱등 키의 보존 기간·물리 purge·복구 정책은 운영 결정 미정이다. 현재 자동 파기·복구 작업이 없다.

## 변경 이력

| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| 2026-11-21 | 개인 CRUD 중 등록·상세·수정 및 WBS 실시간 통합 조회 초안. 3개 미결정 명시 | SCRUM-111, FR-PSN-01 |
| 2026-11-22 | 사용자 결정 반영: 소프트 삭제·감사/재전송 부활 차단, 임박 기본3일, 승인 원천 후속 연결 | SCRUM-111, V14 |
