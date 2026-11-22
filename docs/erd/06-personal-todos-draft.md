# 06. 개인 업무 (ERD) — 개인 TODO 보완 초안

> 상태: Draft · 담당자: 미정 · 마지막 검토일: 2026-11-22
> 요구사항·Jira: FR-PSN-01, SCRUM-111 · 마이그레이션: V13·V14 (운영 배포 전 조건 있음)
> Confluence 기존 06 전체 도메인 ERD 원본 다운로드가 실패해 만든 개인 TODO 보완본이다. 개인 일정·업무일지·노트 등 기존 전체 도메인을 대체하지 않는다.

## 1. ERD

- `users` 1 : N `personal_todos` (`user_id`). 원본 보완 그림: `06-personal-todos.drawio`.
- 개인 항목만 저장한다. 배정 업무는 조회 시 `wbs_tasks`와 합치며 별도 FK·복제 행을 만들지 않는다. 승인 요청 원천은 사용자가 후속 연결을 승인했다.
- 테이블 색상: 사용자 참조 파랑(global), 개인 TODO 초록(coreviaai_be).
- 기존 첨부 원본 다운로드 및 draw.io 전용 PNG 내보내기를 수행할 수 없어 보완 `.drawio`를 로컬 `docs/erd/`에 보관한다. 기존 Confluence 전체 ERD 이미지는 보존한다. 정식 원본·PNG 동기화는 미완료다.

## 2. 테이블 정의

### personal_todos — 개인 TODO (coreviaai_be), V13·V14

| 컬럼 | 타입 | NULL | 기본값 | 제약 | 설명 |
| --- | --- | --- | --- | --- | --- |
| `id` | BIGINT | N | IDENTITY | PK | 개인 항목 ID |
| `user_id` | BIGINT | N | | FK → users.id | 소유자 |
| `title` | VARCHAR(200) | N | | | 제목. 양끝 공백 제거 |
| `due_date` | DATE | Y | | | 마감일, KST 달력 의미. API 허용 0001~9999년 |
| `done` | BOOLEAN | N | FALSE | CHECK | 완료 여부 |
| `done_at` | TIMESTAMPTZ | Y | | CHECK | done이면 필수, 미완료면 null. 주입 Clock UTC |
| `deleted_at` | TIMESTAMPTZ | Y | NULL | | 소프트 삭제 시각. V14 추가, 주입 Clock UTC |
| `request_id` | VARCHAR(36) | N | | UK(user_id, request_id) | 클라이언트 생성 UUID, 불변 |
| `request_hash` | VARCHAR(64) | N | | | 정규화한 최초 제목·마감 SHA-256. 불변, 로그 미기록 |
| `created_at` | TIMESTAMPTZ | N | | | JPA auditing UTC |
| `updated_at` | TIMESTAMPTZ | N | | | JPA auditing UTC |

기존 계획 8개 컬럼에 멱등성 필수 지침을 충족하기 위한 `request_id`·`request_hash`를 추가했다. 제목 중복은 유효하므로 제목을 자연키로 쓰지 않는다. 요청 해시를 최초 입력으로 고정해, 내용 수정·완료 이후에도 동일 키 재전송이 새 TODO를 만들지 않게 한다. 개인 수정은 행 잠금으로 순차 처리하며 version 컬럼을 추가하지 않는다.

## 3. 관계·삭제 규칙

| FK | 관계 | 부모 삭제 시 |
| --- | --- | --- |
| `fk_personal_todos_user` | users 1 : N personal_todos | NO ACTION |

사용자가 개인 TODO의 **소프트 삭제**를 승인했다. 본인행 잠금 후 deleted_at을 표시하고 PERSONAL_TODO_DELETED 감사 로그를 같은 트랜잭션에 기록한다. 삭제된 본인 행의 반복 DELETE는204·감사 중복 없음, GET/PATCH는404다. 모든 개인 목록은 deleted_at IS NULL로 제한하며 WBS는 변경하지 않는다. 다른 개인 업무 도메인의 삭제 정책을 확정하는 것은 아니다.

삭제행도 request_id/request_hash와 유일 키를 유지한다. 같은 생성 키·최초 입력 재전송은409 TODO_DELETED, 다른 입력은409 TODO_REQUEST_CONFLICT. 삭제행을 부활·복제하지 않는다. 보존 기간·물리 purge·복구 정책은 미정이며 자동 작업을 만들지 않았다. 개인 데이터 원문·요청 키·해시는 감사 로그에 저장하지 않고 ownerUserId만 details에 남긴다.

## 4. 인덱스·제약

| 이름 | 대상 | 용도 |
| --- | --- | --- |
| `idx_personal_todos_user_id_due_date` | personal_todos(user_id, due_date) | 본인 목록·날짜 정렬 |
| `uk_personal_todos_user_id_request_id` | personal_todos(user_id, request_id) | 소유자별 생성 요청 중복 방지 |
| `ck_personal_todos_done_at` | done·done_at | 완료/미완료 시각 무결성 |
| `idx_personal_todos_user_id_deleted_at_done_due_date` | personal_todos(user_id, deleted_at, done, due_date) | V14, 본인·미삭제·완료 여부 필터 및 날짜 조회 |

기존 WBS의 `idx_wbs_tasks_assignee_user_id`, 프로젝트 멤버 유일 키를 이용해 통합 목록을 제한한다. 한 번의 제한된 UNION 쿼리로 페이지 size+1건만 반환하며 항목별 추가 조회는 없다. PostgreSQL 실제 대량 데이터 실행 계획은 미검증이다.

## 5. 배포·복구

- V13은 별도 진행 중인 V8~V12 번호와의 충돌을 피하기 위한 예약 번호다. 현재 테스트 기준 원격 develop에는 V1~V7만 있다.
- **운영에는 V8~V12를 통합·승인된 순서로 먼저 migrate한 뒤 V13을 적용해야 한다. V13만 먼저 배포하지 않는다.** Flyway 기본 `outOfOrder=false`에서는 나중에 V8~V12가 들어올 때 검증·적용이 실패한다. 이미 적용한 번호를 바꾸거나 outOfOrder를 임의로 켜지 않는다.
- 새 테이블만 추가하는 expand migration이라 기존 행·V7 애플리케이션 동작에는 영향이 없다. 테이블 추가 후 새 TODO 코드를 배포한다.
- V13은 수정하지 않는다. V14는 nullable deleted_at만 추가해 기존행을 활성(null)로 유지한다. 배포는 V8~V12 → V13 → V14 → 새 API 순서다. 구 API는 삭제행을 필터하지 않으므로 소프트 삭제 활성화 후 구/신 API를 동시에 서비스하지 않는다.
- 소프트 삭제 활성화 후 구 리더로 코드 rollback하면 삭제행이 노출된다. DELETE를 비활성화하고 삭제행 필터를 유지하는 리더로 roll-forward한다. deleted_at 제거·자동 복구·purge는 하지 않는다.
- 복구는 TODO API를 비활성화하고 데이터를 백업·보존한 뒤 새 migration으로 처리한다. 실제 개인 데이터를 보존해야 하므로 자동 DROP·삭제 rollback은 실행하지 않았다.

## 변경 이력

| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| 2026-11-21 | 개인 TODO 및 멱등성 보완 초안. 삭제·승인 미결, V13 배포 순서 주의 명시 | SCRUM-111 |
| 2026-11-22 | 개인 TODO 소프트 삭제 승인 반영, V14 deleted_at·인덱스·감사·멱등키 보존·배포호환성 명시 | SCRUM-111 |
