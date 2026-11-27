<!-- Confluence Sequence 본문과 같은 판정 순서. develop 50f96d2076b09500c986c684d0468f430a00601e. draw.io·PNG는 이 파일에 넣지 않았고, 첨부 완료로 보지 않는다. -->

# 06. 개인 업무 (Sequence)
상태: 현재 구현 대조 · 담당자: 미정 · 마지막 검토일: 2026-11-27. 기준 소스: develop 50f96d2076b09500c986c684d0468f430a00601e (실제 Controller·Service·Policy·Repository·ExceptionHandler 대조).
공통: 기존 JWT·세션 보안 체인과 입력 검증을 먼저 통과한다. 오류는 서비스 표준 ApiResponse이며 본문에 내부 스택·비밀값을 노출하지 않는다. 아래는 API 계약을 새로 바꾸는 문서가 아니라 현재 구현 순서를 정정한 기록이다.
기준: 요구사항명세 최종안: 전사관리 · 3.3 권한 매트릭스. 도식의 화살표는 호출·응답이며 노란 상자는 분기·트랜잭션 조건이다.
## 06-1. 개인 TODO 생성 · 동시 재전송
API: POST /api/me/todos
참여자: FE / 인증·Controller, PersonalTodoService, CoreviaExceptionHandler, PostgreSQL
### 핵심 포인트
날짜 범위 검증 뒤 본인 users 행을 EntityManager.find(...PESSIMISTIC_WRITE)로 잡는다. 해당 사용자가 없으면404 USER_NOT_FOUND다. 생성 내용 해시는 최초 제목·마감일 기준으로 불변이며 같은키·다른내용409가 삭제행409보다 우선이다. 신규 생성 flush뒤 refresh로 DB timestamp 정밀도를 읽어 재전송 응답을 안정화한다.
그림 원본: docs/sequence/06-1-personal-todo-create.drawio / 내보낸 PNG: 06-1-personal-todo-create.png. 두 파일은 실제 생성·가독성 검수 완료. 현재 이 절의 최신 그림 첨부·본문 삽입은 대기 상태이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. FE / 인증·Controller → PersonalTodoService: principal 본인 ID·UUID·제목·날짜 검증 → create
1. PersonalTodoService → PostgreSQL: BEGIN · users 본인 행 PESSIMISTIC_WRITE 잠금
1. PersonalTodoService → PostgreSQL: (user_id, request_id) 조회 · 불변 생성 내용 해시 비교
1. 같은 키 다른 내용: 409 TODO_REQUEST_CONFLICT가 삭제 여부보다 우선. 같은 키·같은 내용 삭제행: 409 TODO_DELETED.
1. PersonalTodoService → PostgreSQL: [신규] INSERT personal_todos + flush + refresh (DB 시간 정밀도)
1. PersonalTodoService → FE / 인증·Controller: COMMIT 후 201 생성 응답
1. [동일 활성 요청] 최초 생성 내용·createdAt 기반 응답을 복원해 201. 수정/완료 후에도 재전송은 최초 응답이며 새 행 없음.
1. PersonalTodoService → CoreviaExceptionHandler: [잠금 실패] Spring 동시성 예외 또는 raw JPA LockTimeout/Pessimistic/Optimistic
1. CoreviaExceptionHandler → FE / 인증·Controller: 409 CONCURRENT_MODIFICATION (제목·requestId·해시 노출 없음)
1. 생성은 users → personal_todos 순서. 본인 userId는 요청으로 받지 않음. 요청 원문/제목을 감사·일반 로그에 기록하지 않음.

## 06-2. 내 TODO 통합 조회 · 현재 권한 재판정
API: GET /api/me/todos
참여자: FE / Controller, TodoFeedService, Access / Visibility, PostgreSQL
### 핵심 포인트
정렬 복합키는 urgency·due_sort·source_order·id이며 무기한 due_sort는9999-12-31이다. 기본3일 임박 범위는 사용자 확정이며 dueSoonDays는1~365다. 개인 항목은 완료 여부 양쪽에서 소프트 삭제를 제외한다. SQL은 enum 이름을 파라미터로 전달하며 CANCELLED 프로젝트·WBS/KANBAN 모두 비활성인 TASK를 제외한다. 첫페이지 KST 오늘은 커서에 고정하지만 사용자의 현재 역할·멤버·부서 범위는 매번 새로 판정한다.
그림 원본: docs/sequence/06-2-personal-todo-feed.drawio / 내보낸 PNG: 06-2-personal-todo-feed.png. 두 파일은 실제 생성·가독성 검수 완료. 현재 이 절의 최신 그림 첨부·본문 삽입은 대기 상태이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. FE / Controller → TodoFeedService: 인증·size 1~100·dueSoonDays 1~365(기본3)·done 검증
1. TodoFeedService → TodoFeedService: cursor 본인·done·기간·형식 검증, 첫 페이지 KST 오늘 계산
1. TodoFeedService → Access / Visibility: 현재 User 역할·경영진 본인+하위 부서 범위 재조회
1. TodoFeedService → PostgreSQL: 한 번의 size+1 제한 UNION: 본인 PERSONAL + 현재 담당 TASK
1. PERSONAL: deleted_at IS NULL + done 필터. TASK: done=false·status!=DONE·현재 멤버·WBS/KANBAN 활성·프로젝트 CANCELLED 아님.
1. 시스템 관리자는 PERSONAL만. 일반 USER는 멤버 조건, 경영진 TASK는 전사공개 또는 복수 주관부서 중 소속범위 추가 조건.
1. PostgreSQL → TodoFeedService: 오늘 → D+1~D+3 → 지연 → 그밖 마감 → 무기한 / 날짜·source·id 안정 정렬
1. TodoFeedService → FE / Controller: 200 source 포함 items·nextCursor·hasNext (다음 페이지 오늘 기준 고정)
1. 매 페이지 현재 권한·배정·기능 재판정. 커서가 권한을 보존하지 않음. 완료·해제·제외·권한 변경 커밋 뒤 다음 조회에서 정리.
1. 원천 조회 방식: 복제/정리 배치 없음. 1분 내 표시에는 FE 재조회 필요. 승인 요청 원천은 사용자 승인에 따라 후속 연결.

## 06-3. 개인 TODO 삭제 · 소프트 삭제와 재전송
API: DELETE /api/me/todos/{todoId}. 응답 204 No Content.
참여자: FE, PersonalTodoController, PersonalTodoService, PersonalTodoRepository, AuditLogger, PostgreSQL
### 핵심 포인트
- 삭제는 deletedAt만 채운다. 행과 requestId·생성 해시는 남는다.
- findOwnedForUpdate는 본인 행만 잠근다. 없거나 소유자가 아니면 404 TODO_NOT_FOUND.
- softDelete가 처음이면 flush·refresh 후 PERSONAL_TODO_DELETED 감사를 같은 트랜잭션에 남긴다. 이미 삭제된 행이면 감사 없이 반환하고 204다.
- 같은 requestId 재전송은 06-1 순서다. 해시가 다르면 409 TODO_REQUEST_CONFLICT가 먼저이고, 해시가 같고 deletedAt이 있으면 409 TODO_DELETED. 행을 되살리지 않는다.

### 판정 순서
1. FE → DELETE /api/me/todos/{todoId}. 컨트롤러는 AuditActor와 todoId만 넘긴다.
1. PersonalTodoService.delete 트랜잭션 시작. findOwnedForUpdate(todoId, actor.userId). 없으면 404 TODO_NOT_FOUND와 ROLLBACK.
1. softDelete: deletedAt이 이미 있으면 false. 감사·추가 UPDATE 없이 커밋하고 204.
1. deletedAt이 없으면 현재 시각을 넣고 flush·refresh.
1. PERSONAL_TODO_DELETED 감사를 같은 트랜잭션에 기록. 감사 실패면 삭제 표시도 ROLLBACK.
1. 커밋 후 204. 이후 같은 생성 requestId는 새 행을 만들지 않고 409 TODO_DELETED.

그림은 이번 작업에서 만들지 않았다. draw.io와 PNG는 사용자가 첨부한다.
## 06-4. 업무일지 저장
- API: `PUT /api/me/worklogs/{logDate}`
- 참여자: 사용자, 인증 필터, WorklogController, WorklogService, EntityManager, WorklogRepository, PostgreSQL, GlobalExceptionHandler

### 핵심 포인트
- 인증 주체의 userId를 사용하며 본문 10,000자와 날짜 형식을 검사한다.
- 한 DB 트랜잭션에서 users 행을 먼저 PESSIMISTIC_WRITE로 잠근 뒤 worklogs를 조회한다. 같은 사용자·날짜 첫 요청은 직렬화되고 유일 제약으로 중복을 막는다.
- 직접 EntityManager 호출의 JPA 잠금 timeout·실패는 Spring 저장소 예외 변환을 거치지 않으므로 CONCURRENT_MODIFICATION으로 명시 변환한다. 롤백 후 409를 반환하며 예상 밖 500·에러 로그 경로로 내려가지 않는다.
- 같은 내용의 재전송은 dirty checking이 UPDATE를 생략한다. flush·refresh 후 DB 정밀도의 시각을 응답한다.
- 서로 다른 내용의 동시 저장은 직렬화된 마지막 쓰기가 반영된다. 낙관적 version 비교는 현재 계약에 없다.

### 판정 순서
1. 미인증: 401. 날짜·본문 형식 오류: 400.
1. 서비스 트랜잭션 시작 → users 행 잠금.
1. 소유자 없음: 404 USER_NOT_FOUND, 롤백.
1. 잠금 timeout 또는 획득 실패: 원인을 내부에 보존해 CustomException으로 변환 → 롤백 → 409 CONCURRENT_MODIFICATION. 본문·DB 상세 비노출.
1. 잠금 성공: userId와 logDate로 조회 → 없으면 INSERT, 있으면 내용 교체 → flush·refresh.
1. 트랜잭션 커밋과 잠금 해제 후 200 응답. 두 번째 요청은 커밋 뒤 잠금을 얻어 기존 행을 읽는다.

이 06-4는 날짜별 개인 본문 저장만 설명한다. 업무 연결·진척률·상위 집계·PM 조회는 아래 06-5~06-8의 별도 구현 흐름을 따른다. 이 06-4 본문은 날짜별 개인 본문만 다룬다. 연결 저장·개인 연결 조회·PM 조회는 06-5·06-7·06-8이다. 로컬 사본은 docs/sequence/06-personal-sequence.md. 그림은 이번 작업에서 만들지 않았다.
## 06-5. 업무일지 연결 업무 저장 · 원자성과 멱등성
API: `PUT /api/me/worklogs/{date}/tasks/{taskId}`
참여자: 사용자, WorklogTaskController, WorklogTaskService, WbsTaskAccessPolicy, ProjectAccessPolicy, TaskDetailService, EntityManager, WorklogRepository, WorklogTaskRepository, DB, GlobalExceptionHandler
### 핵심 포인트
- FR-PSN-03. USER 본인 담당 업무에만 저장한다. 업무 수정 가능 여부, 프로젝트·업무 ID, 본인 담당을 먼저 검사한다. completed가 true이고 progress가 100이 아니면 400 WBS_COMPLETED_PROGRESS_REQUIRED. completed가 false이고 업무 status가 이미 DONE이면 같은 400으로 거부한다. 이 분기는 사용자 행 잠금보다 앞이다.
- 고정 잠금 순서는 프로젝트 행 → 사용자 행이다. 프로젝트 잠금은 WbsTaskAccessPolicy의 편집자 권한 획득 경로를 따르고, 사용자 행은 PESSIMISTIC_WRITE로 획득한다.
- 동일 내용·진척률·소요 시간·완료 여부 재전송은 기존 기록을 반환하며 진척률이나 시간을 재가산하지 않는다.
- 내용이 다르면 연결 기록 version과 업무 taskVersion을 검증한 후 TaskDetailService.complete/updateProgress로 업무 진척률·완료 및 상위 집계를 반영한다. 모든 변경은 단일 트랜잭션이다.

### 판정 순서
1. FE → WorklogTaskController: 인증된 업무 연결 upsert(date,taskId,projectId,progress,spentMinutes,completed,version,taskVersion)
1. Controller → WorklogTaskService: BEGIN, 시스템 역할 USER 확인
1. WorklogTaskService → WbsTaskAccessPolicy/Repository: 시스템 역할이 USER가 아니면 403 WBS_TASK_ASSIGNEE_ONLY. 공개 범위·편집 권한·업무 조회·본인 담당을 검사한다. completed=true이고 progress!=100, 또는 completed=false이고 status가 DONE이면 400 WBS_COMPLETED_PROGRESS_REQUIRED. 실패 시 잠금 전에 ROLLBACK
1. WorklogTaskService → EntityManager/DB: 프로젝트 락 확보 후 users 행 PESSIMISTIC_WRITE; 잠금 실패는 409 CONCURRENT_MODIFICATION
1. WorklogTaskService → WorklogRepository: 해당 날짜 본인 일지 조회 또는 생성; WorklogTaskRepository에서 기존 업무 연결 조회
1. alt 동일 값: 변경 없이 기존 WorklogTaskResult 200 반환
1. alt 변경: 기존 연결 version 불일치면 409 및 전체 롤백
1. WorklogTaskService → TaskDetailService: 완료 요청이면 complete(taskVersion), 그 외 updateProgress(progress,taskVersion) 호출 → 업무 및 상위 진척 이력 처리
1. WorklogTaskService → WorklogTaskRepository: 연결 업무 INSERT 또는 UPDATE, saveAndFlush
1. DB → Controller → FE: COMMIT 및 200 WorklogTaskResult; 어느 단계든 오류면 업무 변경·일지 연결·이력 모두 ROLLBACK

그림 작성 기준: 기존 Sequence 문서와 동일하게 참여자 레인·방향성 호출 화살표, alt 분기 및 트랜잭션·실패 조건은 노란 상자. draw.io와 PNG는 사용자가 추후 첨부할 예정이며 현재 첨부 완료 상태가 아니다.
## 06-6. 개인 통합 일정 조회 · 개인 일정과 담당 업무
API: `GET /api/me/calendar`
참여자: 사용자, CalendarController, CalendarService, QueryRangePolicy, ProjectAccessPolicy, ProjectVisibilityPolicy, CalendarRepository, DB
### 핵심 포인트
- FR-PSN-02. from/to는 Instant이며 기간 검증 후 page 기본 0·size 기본 20(offset)을 적용한다.
- CalendarRepository는 개인 일정 및 담당 업무 원천을 통합 조회한다. 현재 사용자 역할과 경영진 소속 부서 범위를 전달한다.
- size+1 조회 결과에서 size개만 반환하고 hasNext를 계산한다. 조회는 readOnly 트랜잭션이다.

### 판정 순서
1. FE → CalendarController: GET from, to, page, size (인증 완료)
1. Controller → CalendarService: find(userId,from,to,page,size)
1. CalendarService → QueryRangePolicy: 조회 기간·순서 검증
1. CalendarService → ProjectAccessPolicy: 사용자 조회·시스템 역할 확인
1. 역할 분기. SYSTEM_ADMIN은 allowTasks=false라 개인 일정만 조회하고 프로젝트 담당 업무는 빼다. EXECUTIVE는 allowTasks=true, ordinary=false이고 담당 업무는 취소되지 않은 프로젝트 중 전사 공개이거나 소속 부서 범위에 속하며 WBS 또는 KANBAN이 켜져 있고 본인이 멤버인 것만. 그 외 USER는 ordinary=true라 같은 멤버·기능·취소 제외 조건의 담당 업무를 포함한다
1. CalendarService → CalendarRepository/DB: 개인 일정+담당 업무 기간 조건 및 접근 조건 적용한 offset 조회(size+1)
1. CalendarRepository → CalendarService: 정렬된 통합 결과
1. CalendarService → Controller → FE: CalendarPage(items,hasNext), 200; 잘못된 기간은 검증 오류

그림 작성 기준: 기존 Sequence 문서와 동일하게 참여자 레인·방향성 호출 화살표, alt 분기 및 트랜잭션·실패 조건은 노란 상자. draw.io와 PNG는 사용자가 추후 첨부할 예정이며 현재 첨부 완료 상태가 아니다.
## 06-7. 개인 연결 업무 조회
API: GET /api/me/worklogs/{date}/tasks. page 기본 0, size 기본 20, 최대 100.
참여자: FE, WorklogTaskController, WorklogTaskService, WorklogTaskRepository, WorklogRepository, PostgreSQL
### 핵심 포인트
- 인증 주체의 userId와 날짜로 본인 연결만 읽는다. 다른 사용자 행은 쿼리에 없다.
- readOnly 트랜잭션. 업무 진척률을 바꾸지 않는다.

### 판정 순서
1. FE → GET /api/me/worklogs/{date}/tasks.
1. WorklogTaskService.findOwned(userId, date, page, size).
1. 본인 worklog_tasks를 size+1이 아닌 Spring slice로 읽는다.
1. 연결된 업무일지 id로 작성자·날짜를 일괄 조회해 WorklogTaskPage를 만들고 200. 개인 본문 content는 이 응답 모델에 넣지 않는다.

그림은 이번 작업에서 만들지 않았다.
## 06-8. PM 프로젝트 업무일지 조회
API: GET /api/projects/{projectId}/worklogs?from=&to=. page 기본 0, size 기본 20, 최대 100.
참여자: PM FE, WorklogTaskController, WorklogTaskService, ProjectAccessPolicy, QueryRangePolicy, PostgreSQL
### 핵심 포인트
- 시스템 역할이 USER가 아니면 403 PROJECT_PM_ONLY. 시스템 관리자·경영진은 이 API로 프로젝트 업무일지를 보지 않는다.
- 이어서 getManagedProject. 공개 범위가 아니면 404 PROJECT_NOT_FOUND, PM이 아니면 403 PROJECT_PM_ONLY.
- from~to는 QueryRangePolicy.requireDateRange. 개인 업무일지 본문은 직렬화하지 않고 연결 업무 기록만 반환한다.

### 판정 순서
1. FE → GET /api/projects/{projectId}/worklogs.
1. viewer의 SystemRole이 USER가 아니면 403 PROJECT_PM_ONLY.
1. getManagedProject(viewerId, projectId).
1. 기간이 유효하지 않으면 검증 오류.
1. 그 프로젝트의 연결 업무를 페이지로 읽고, 업무일지 id로 작성자·날짜만 붙여 200. 일지 본문은 응답에 넣지 않는다.

그림은 이번 작업에서 만들지 않았다. draw.io와 PNG는 사용자가 첨부한다.
## 변경 이력
November 22, 2026 — 현재 구현 대조: 복수 PM·복수 부서/생성 권한 개방·폐지 위임·관리자 복구, WBS 상한/토큰/잠금, COUNT WIP, TODO 멱등/소프트 삭제/raw JPA409 중 해당 흐름 정정. 모든 그림은 draw.io와 PNG를 동일 좌표 모델에서 생성. 새 첨부는 진행 상태에 따라 별도 확인한다.
| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| 2026-11-21 | 개인 생성 멱등·동시 요청 잠금 순서, 통합 조회 권한 재판정 초안 | SCRUM-111 |
| 2026-11-22 | 사용자 결정 반영: 소프트 삭제·감사 원자성/재전송 부활 차단, 기본3일, 승인 원천 후속 연결. 2026-11-27 develop 50f96d2076b09500c986c684d0468f430a00601e: 06-3 삭제 시퀀스 복구. 06-5에 이미 DONE인 업무를 completed=false로 저장하면 400. 06-6에 시스템 관리자는 개인 일정만. 06-7·06-8을 추가해 06-4의 참조와 문서 구성을 맞춤. | SCRUM-111, V14 |
