<!-- Confluence Sequence 본문과 같은 판정 순서. develop 50f96d2076b09500c986c684d0468f430a00601e. draw.io·PNG는 이 파일에 넣지 않았고, 첨부 완료로 보지 않는다. -->

# 04. WBS·일정 (Sequence)
상태: 현재 구현 대조 · 담당자: 미정 · 마지막 검토일: 2026-11-27. 기준 소스: develop 50f96d2076b09500c986c684d0468f430a00601e. 04-1 generate·contextReader·getDraftableProject, 04-2 apply, 04-3 move, 04-4 replace, 04-5 create, 04-6 runDaily·find를 다시 읽었다 (실제 Controller·Service·Policy·Repository·ExceptionHandler 대조).
공통: 기존 JWT·세션 보안 체인과 입력 검증을 먼저 통과한다. 오류는 서비스 표준 ApiResponse이며 본문에 내부 스택·비밀값을 노출하지 않는다. 아래는 API 계약을 새로 바꾸는 문서가 아니라 현재 구현 순서를 정정한 기록이다.
기준: 요구사항명세 최종안: 전사관리 · 3.3 권한 매트릭스. 도식의 화살표는 호출·응답이며 노란 상자는 분기·트랜잭션 조건이다.
## 04-1. AI WBS 초안 생성
API: POST /api/projects/{projectId}/wbs/ai-draft
참여자: PM FE / Controller, GenerationService, AI Client / Generator, PostgreSQL / Redis
### 핵심 포인트
핵심 모듈은 WbsDraftContextReader, WbsDraftAccessPolicy, WbsDraftAiClient와 aiserver.domain.wbs.api.WbsDraftGenerator다. AI_NOT_READY503, AI_GENERATION_FAILED502, AI_TIMEOUT504로 오류를 변환한다. 요청 원문을 저장하지 않고 길이·결과·제안 개수를 감사에 남긴다. AI 실패의 ai_drafts/감사 기록 실패가 원래 AI 오류를 덮지 않는다.
그림 원본: docs/sequence/04-1-wbs-ai-draft-generate.drawio / 내보낸 PNG: 04-1-wbs-ai-draft-generate.png. 문서에서 가리키는 신규 그림은 아직 Confluence 본문에 첨부되지 않았다. draw.io와 PNG 실제 파일 존재 여부는 원격 저장소 기준으로 별도 확인해야 한다.
### 판정 순서
1. PM FE / Controller → GenerationService: 입력 검증 후 generate(actor, projectId, command)
1. GenerationService → PostgreSQL / Redis: 읽기 트랜잭션: 공개 범위 404 → PM 아니면 403 → 단계가 0개면 404 PHASES_NOT_AVAILABLE → WBS 기능이 없으면 404 FEATURE_DISABLED. 운영 방식 enum은 보지 않는다 · 문맥 조회
1. 읽기 트랜잭션 종료 후 AI 호출. LLM 대기 중 DB 트랜잭션을 유지하지 않음.
1. GenerationService → AI Client / Generator: aiserver api 계약 WbsDraftGenerator 호출 (업무 모듈은 재시도 없음)
1. AI Client / Generator → GenerationService: 제안 또는 표준 AI 오류 (현재 NOT_READY 구현은 503)
1. GenerationService → PostgreSQL / Redis: 성공: ai_drafts DRAFT 기록 (자체 트랜잭션)
1. GenerationService → PostgreSQL / Redis: Redis draftToken 발급: projectId·PM·aiDraftId, TTL 1시간
1. GenerationService → PostgreSQL / Redis: 별도 트랜잭션 AI_WBS_DRAFT_REQUESTED 감사 (원문 미기록)
1. GenerationService → PM FE / Controller: 200 초안·draftToken·expiresAt (WBS 저장 없음)
1. AI 오류: ai_drafts FAILED와 실패 감사를 각각 시도하고 원래 AI 오류 반환. 생성 재전송은 새 초안·새 토큰.

## 04-2. AI WBS 초안 승인 반영 · 원자적 토큰
API: POST /api/projects/{projectId}/wbs/ai-draft/apply
참여자: PM FE / Controller, ApplyService / Policy, Redis tokenStore, PostgreSQL
### 핵심 포인트
실제 상한은 WbsTaskService.MAX_ITEMS_PER_PROJECT=2000, MilestoneService.MAX_MILESTONES_PER_PROJECT=200이다. 초과는 각각409 WBS_TASK_LIMIT_EXCEEDED / MILESTONE_LIMIT_EXCEEDED다. WbsDraftValidator 뒤 WbsItemWriter가 저장하고 AiDraftRecorder 결정·감사를 같은 트랜잭션에서 수행한다. 토큰 consume 전 권한 실패는 토큰을 남긴다. 트랜잭션 내부 재확인은 프로젝트 잠금보다 앞에 실행되는 현재 코드 순서이며, 잠금 이후 별도 권한 재검사까지 수행한다고 확대해 설명하지 않는다.
그림 원본: docs/sequence/04-2-wbs-ai-draft-apply.drawio / 내보낸 PNG: 04-2-wbs-ai-draft-apply.png. 문서에서 가리키는 신규 그림은 아직 Confluence 본문에 첨부되지 않았다. draw.io와 PNG 실제 파일 존재 여부는 원격 저장소 기준으로 별도 확인해야 한다.
### 판정 순서
1. PM FE / Controller → ApplyService / Policy: 승인 항목 형식·길이·개수 검증 후 apply
1. ApplyService / Policy → PostgreSQL: 토큰 소비 전 공개 범위 404 → PM 아니면 403 → 단계가 0개면 404 PHASES_NOT_AVAILABLE → WBS 기능이 없으면 404 FEATURE_DISABLED. 운영 방식 enum은 보지 않는다
1. ApplyService / Policy → Redis tokenStore: 소유자·만료·반영완료 조회 → consume 원자적 삭제
1. 잘못된 소유/만료 404 AI_DRAFT_EXPIRED, 이미 반영/consume 경쟁 409 AI_DRAFT_ALREADY_APPLIED.
1. ApplyService / Policy → PostgreSQL: TransactionTemplate BEGIN · 권한/기능 재확인 → projects FOR UPDATE
1. ApplyService / Policy → PostgreSQL: 작업+하위작업 총 2,000 / 마일스톤 총 200 상한 검사
1. ApplyService / Policy → ApplyService / Policy: 멤버·tempId·참조·기간·순환 검증 (프로젝트 날짜 있는 경계만). 이어서 요청 phaseId가 그 프로젝트 단계가 아니면 400 INVALID_WBS_PHASE
1. ApplyService / Policy → PostgreSQL: 작업·하위작업·선후행·마일스톤 저장
1. ApplyService / Policy → PostgreSQL: ai_drafts ACCEPTED/PARTIALLY_ACCEPTED + APPLIED 감사
1. PostgreSQL → ApplyService / Policy: 모두 COMMIT (단계 연결 없음, 하위작업 기반 부모 기간 산출)
1. ApplyService / Policy → Redis tokenStore: 커밋 후 markApplied (표시 실패는 성공 응답 유지)
1. ApplyService / Policy → PM FE / Controller: 200 정식 ID 매핑 · aiserver 재호출 없음
1. 트랜잭션 실패: ROLLBACK 후 토큰 issue로 복구 시도, 원래 오류 재전달. 복구도 실패하면 초안 재생성. markApplied 실패 뒤 재전송은 409 대신 404일 수 있음.

## 04-3. WBS 항목 이동 · 프로젝트 잠금
API: PUT /api/projects/{projectId}/wbs/tasks/{taskId}/position
참여자: PM FE / Controller, WbsTaskService, AccessPolicy / Editor, PostgreSQL
### 핵심 포인트
index는0~2000, version은0이상이다. 같은 위치 성공은 version검사보다 먼저다. 이동 엔티티 변경과 부모 기간 산출을 먼저 flush하고, 영속성 컨텍스트를 비우는 형제 순서 일괄 갱신을 마지막에 수행한다. 부모 기간이 바뀌면 그 부모 version은 증가하며 화면은 응답의 taskId·tasks[]로 다시 그린다.
그림 원본: docs/sequence/04-3-wbs-task-move.drawio / 내보낸 PNG: 04-3-wbs-task-move.png. 문서에서 가리키는 신규 그림은 아직 Confluence 본문에 첨부되지 않았다. draw.io와 PNG 실제 파일 존재 여부는 원격 저장소 기준으로 별도 확인해야 한다.
### 판정 순서
1. PM FE / Controller → WbsTaskService: parentTaskId·index·version 검증 후 move
1. WbsTaskService → AccessPolicy / Editor: 프로젝트 공개범위404 → WBS 기능404
1. AccessPolicy / Editor → PostgreSQL: projects FOR UPDATE → 현재 멤버 역할 확인
1. WbsTaskService → AccessPolicy / Editor: PM 전용403 → 이동 업무 조회404
1. WbsTaskService → PostgreSQL: 새 부모·형제 조회 · 자기부모/3단계·인덱스 검사
1. 같은 부모·같은 현재 index는 version 미검사, 변경 없이 결과 반환. 실제 이동일 때만 version 충돌409.
1. WbsTaskService → PostgreSQL: 부모/순서 이동 + flush + 변경 이력
1. WbsTaskService → PostgreSQL: 부모 변경 시 옛/새 부모 기간 재산출 · flush
1. WbsTaskService → PostgreSQL: 도착/출발 형제 순서 1부터 일괄 갱신 (형제 version은 올리지 않음)
1. PostgreSQL → WbsTaskService: COMMIT · 실패는 이동·부모 기간·이력 함께 ROLLBACK
1. WbsTaskService → PM FE / Controller: 200 이동 결과 (다른 변경 API와 같은 프로젝트 잠금)

## 04-4. 선행 작업 전체 교체 · 순환 차단
API: PUT /api/projects/{projectId}/wbs/tasks/{taskId}/predecessors
참여자: PM·담당 개발자/QA FE / Controller, TaskDependencyService, AccessPolicy / Editor, PostgreSQL
### 핵심 포인트
같은 프로젝트 행 잠금 안에서 현재 관계와 요청 관계를 합친 전체 그래프를 위상 정렬한다. 자기 참조·다른 프로젝트 작업·없는 작업은 동일한 400으로 처리해 자원 존재를 노출하지 않는다. 검증이 끝난 뒤 빠진 관계만 삭제하고 새 관계만 추가하며, 실패 시 전부 롤백한다.
### 판정 순서
1. FE / Controller → TaskDependencyService: `predecessorTaskIds` 필수·양수·최대 200개 검증
1. TaskDependencyService → AccessPolicy / Editor: 프로젝트 공개범위 404 → WBS 기능 404
1. AccessPolicy / Editor → PostgreSQL: `projects FOR UPDATE` → 현재 멤버 역할 확인
1. TaskDependencyService → PostgreSQL: 후행 작업을 프로젝트 범위로 조회(없거나 다른 프로젝트면 404)
1. TaskDependencyService → Editor: PM 전체, 개발자·QA 본인 담당 후행만 허용(그 밖 403)
1. TaskDependencyService → PostgreSQL: 요청 선행 ID가 같은 프로젝트의 다른 작업인지 개수 쿼리로 검증(실패 400)
1. TaskDependencyService → PostgreSQL: 현재 후행 관계 조회. 요청 집합과 같으면 변경 없이 200
1. TaskDependencyService: 프로젝트 전체 관계에서 해당 후행 간선을 요청 집합으로 치환한 그래프를 위상 정렬(순환 400)
1. TaskDependencyService → PostgreSQL: 빠진 관계 삭제 + 새 관계 추가 + flush
1. PostgreSQL → TaskDependencyService: COMMIT · 실패는 관계 변경 전체 ROLLBACK
1. TaskDependencyService → FE / Controller: 200 선행 작업 목록·현재 날짜 기준 지연 영향

과거 계약 그림·원본 보존 (현재 판정 순서로 사용하지 않음)아래는 이전 게시 그림/첨부다. 단일 PM·canCreateProject·단일 주관 부서·이전 AI 흐름을 포함할 수 있어 현재 구현의 근거로 사용하지 않는다. 현재 흐름은 위 절과 새 draw.io/PNG를 따른다. 첨부 파일과 media ID는 삭제하지 않고 보존했다.
## 04-5. 회의록 등록 · 요청 멱등성과 사용자 행 잠금
API: POST /api/projects/{projectId}/meetings. 참여자: FE, MeetingController, MeetingService, MeetingAccessPolicy, EntityManager(User), MeetingRepository, MeetingCreateRequestRepository, PostgreSQL, GlobalExceptionHandler.
### 핵심 포인트
- 프로젝트 작성 권한 확인 및 요청 본문(참석자·결정사항·액션아이템) 검증 후 사용자 행 PESSIMISTIC_WRITE 잠금.
- projectId+authorUserId+requestId로 기존 요청을 찾는다. 판정 순서는 해시 불일치 → 409 MEETING_REQUEST_CONFLICT, 해시가 같고 meetingId가 null(삭제됨) → 409 MEETING_REQUEST_DELETED, 해시가 같고 회의록이 있음 → 기존 회의록. 최초 생성과 이 재전송 모두 HTTP 201 Created다.
- 신규 회의록·참석자·결정·액션아이템 및 요청키를 한 트랜잭션에서 저장. 유효 사용자 ID를 확인하며 잠금 오류는 409로 변환한다.

### 판정 순서
1. FE → Controller → MeetingService: 작성 요청 + UUID requestId
1. MeetingService → MeetingAccessPolicy: 프로젝트 작성 권한 판정 → 403/404
1. MeetingService: 본문·참석자·결정·액션아이템, 요청 ID 검증
1. MeetingService → EntityManager(User): 작성자 행 PESSIMISTIC_WRITE 잠금, 실패 시 409 CONCURRENT_MODIFICATION
1. MeetingService → MeetingCreateRequestRepository: projectId·authorId·requestId로 기존 요청 조회
1. 기존 요청이 있으면 1) 해시 불일치 → 409 MEETING_REQUEST_CONFLICT 2) 해시 일치이고 meetingId가 null → 409 MEETING_REQUEST_DELETED 3) 해시 일치이고 회의록이 있음 → 기존 상세. 재전송도 201 Created
1. else 신규: 참조 사용자 활성 검사 → MeetingRepository.saveAndFlush 및 관련 항목 반영
1. MeetingCreateRequestRepository.saveAndFlush(requestId, hash, meetingId)
1. COMMIT 후 상세. 최초 생성과 동일 요청 재전송 모두 201 Created. 실패하면 회의록·요청키 전체 ROLLBACK

draw.io 작성 기준: 기존 WBS Sequence와 동일한 레인·호출 화살표, 노란 분기·트랜잭션 조건 상자. 파일은 사용자가 직접 첨부하며 아직 본문에 삽입되지 않았다.
## 04-6. 리스크 일일 배치 · 지연·인력 편중 계산
API: HTTP 호출이 아닌 내부 스케줄 작업. 관련 조회 API: GET /api/projects/{projectId}/risk-overloads. 참여자: Scheduler, RiskAnalysisRunner, RiskAnalysisRunRepository, ProjectRepository, DelayDetectionService, OverloadDetectionService, RiskDetectionRepository, PostgreSQL.
### 핵심 포인트
- app.risk-analysis.enabled=true인 경우 UTC 기본 00:30에 일일 배치를 실행한다. 설정으로 cron을 변경할 수 있다.
- 한국 업무 날짜(BusinessCalendar.today)를 runDate로 사용한다. 같은 날짜 성공 기록이 있으면 스킵하고, FAILED/RUNNING 기록은 재시작한다.
- WBS 기능이 있는 프로젝트 중 COMPLETED/CANCELLED를 제외한다. 대상 프로젝트별 일정 지연과 인력 편중을 감지해 단일 run_id로 결과를 기록한다.
- 실패 시 RuntimeException을 다시 던지지 않고 run을 FAILED로 바꾸며 해당 실행의 감지 결과를 삭제한다.

### 판정 순서
1. Scheduler → RiskAnalysisRunner.runDaily(): 설정된 UTC 시각 작업 시작
1. Runner → DB: 현재 업무일 runDate와 기존 RiskAnalysisRun 조회
1. alt SUCCEEDED: 중복 계산 없이 종료(-1)
1. else FAILED/RUNNING: 이전 감지 결과 삭제 및 run 재시작; 신규이면 RUNNING 행 생성
1. Runner → FeatureRepository/ProjectRepository: WBS 기능의 활성(완료·취소 제외) 프로젝트 ID 조회
1. loop 프로젝트별: DelayDetectionService 지연 계산 → OverloadDetectionService 편중 계산 → RiskDetection 목록 구성
1. Runner → RiskDetectionRepository: 감지 결과 일괄 저장 → run SUCCEEDED
1. alt 계산 RuntimeException: run FAILED, 해당 run 감지 결과 삭제, -2로 종료
1. GET /api/projects/{projectId}/risk-overloads: getVisibleProject 후 resolveScope. 허용되지 않은 역할은 성공 실행이 없어도 403 OVERLOAD_VIEW_FORBIDDEN. 성공한 RiskAnalysisRun이 없으면 빈 결과(오류 아님). 있으면 OVERLOAD만 본다. PM은 담당자 전원, 멤버인 개발자·QA는 본인. 경영진은 소속 범위 부서별 인원 수이고, departmentId가 null인 과부하 인원이 1명 이상이면 부서 목록 뒤에 소속 없음으로 따로 집계한다

원본 그림 첨부는 사용자 진행 예정. 기존 Sequence 문서와 동일하게 참여자 레인과 호출 화살표, alt/loop 및 트랜잭션·오류 조건은 노란 상자로 표현한다.
## 변경 이력
November 23, 2026 — SCRUM-118 / FR-WBS-03: 프로젝트 잠금, 범위 검증, 전체 그래프 순환 차단, 차등 저장·롤백 판정 순서를 04-4에 추가. ERD 변경 없음.
November 22, 2026 — 현재 구현 대조: 복수 PM·복수 부서/생성 권한 개방·폐지 위임·관리자 복구, WBS 상한/토큰/잠금, COUNT WIP, TODO 멱등/소프트 삭제/raw JPA409 중 해당 흐름 정정. 모든 그림은 draw.io와 PNG를 동일 좌표 모델에서 생성. 새 첨부는 진행 상태에 따라 별도 확인한다.
| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| November 21, 2026 | 최초 작성 (흐름 2개). 그림 원본 `docs/sequence/04-1·04-2` | SCRUM-112 |
| November 21, 2026 | 04-1: AI 서버 HTTP 호출 → BE 내 aiserver 모듈 계약 호출로 변경 | SCRUM-112 |
| November 21, 2026 | 04-1·04-2: `ai_drafts` 기록(생성 DRAFT·실패 FAILED·반영 ACCEPTED/PARTIALLY_ACCEPTED) 추가. 그림 다시 올리기 필요(`docs/sequence/04-1·04-2`) | SCRUM-112 |
| November 21, 2026 | 04-3 WBS 항목 이동 흐름 추가. 2026-11-27 develop 50f96d2076b09500c986c684d0468f430a00601e: 04-1·04-2의 폭포수 게이트를 단계 0개 PHASES_NOT_AVAILABLE과 WBS FEATURE_DISABLED로 고침. 04-2에 phaseId 검사 추가. 04-5 최초·재전송 모두 201, 중복 판정을 해시 불일치 → 삭제된 요청 → 기존 반환 순으로 고침. 04-6에 성공 실행 없음·OVERLOAD_VIEW_FORBIDDEN·경영진 소속 없음 집계를 추가. 04-3 move와 04-4 replace 순서는 코드와 같았음. | SCRUM-107 |
