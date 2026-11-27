<!-- Confluence Sequence 본문과 같은 판정 순서. develop 50f96d2076b09500c986c684d0468f430a00601e. draw.io·PNG는 이 파일에 넣지 않았고, 첨부 완료로 보지 않는다. -->

# 03. 프로젝트 (Sequence)
상태: 현재 구현 대조 · 담당자: 미정 · 마지막 검토일: 2026-11-27. 기준 소스: develop 50f96d2076b09500c986c684d0468f430a00601e. 03-1·03-2·03-8·03-9는 이 커밋의 Controller·Service를 다시 읽었다. 03-3·03-4·03-6 본문은 이번 대조에서 서비스 구현을 다시 열지 않아 기존 문장을 유지했다. 03-5는 폐기 API로 본문 절을 복구하지 않고, 03-7은 03-4에 통합된 상태라 별도 절을 두지 않는다. (실제 Controller·Service·Policy·Repository·ExceptionHandler 대조).
공통: 기존 JWT·세션 보안 체인과 입력 검증을 먼저 통과한다. 오류는 서비스 표준 ApiResponse이며 본문에 내부 스택·비밀값을 노출하지 않는다. 아래는 API 계약을 새로 바꾸는 문서가 아니라 현재 구현 순서를 정정한 기록이다.
기준: 요구사항명세 최종안: 전사관리 · 3.3 권한 매트릭스. 도식의 화살표는 호출·응답이며 노란 상자는 분기·트랜잭션 조건이다.
## 03-1. 프로젝트 생성 · 현재 복수 PM 계약
API: POST /api/projects
참여자: FE / Controller, CreationService, Policy / Initializer, PostgreSQL
### 핵심 포인트
생성 화면은 기본 정보 → 템플릿·운영 방식 → 액터 → 기능 구성 → 멤버(선택) → 게이트 승인자(선택)의 6단계다. 주관 부서·기간은 후입력 가능하다. 경영진 생성403·시스템 관리자 프로젝트 경로404다. created_by는 최초 생성자이며 현재 PM 목록은 project_members의 PM 역할로 결정한다.
그림 원본: docs/sequence/03-1-project-create.drawio / 내보낸 PNG: 03-1-project-create.png. 그림은 문서 원본 명칭을 기준으로 안내하며 현재 develop에 해당 원본·PNG의 존재가 확인되지 않았다. 파일 첨부는 사용자 진행 예정이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. FE / Controller → CreationService: 단계형 입력 ①~⑥ 검증·정규화 후 create(actor, command)
1. 트랜잭션 시작 후 첫 판정은 생성 권한이다. requireProjectCreator는 활성 SystemRole.USER만 허용하고, 아니면 403 PROJECT_CREATION_FORBIDDEN. 시스템 관리자는 /api/projects/** 인가에서 404 NOT_FOUND라 이 메서드에 도달하지 않는다. canCreateProject·단일 pm_user_id는 쓰지 않는다.
1. applyTemplatePhases(): phases가 null이면 templateService.defaultPhases로 단계 이름만 명령에 채운다. phases가 있으면 빈 목록을 포함해 요청 값을 유지한다. 이 단계는 메모리에서 이름을 정할 뿐 DB에 쓰지 않는다.
1. 그 다음 creationPolicy.validate: 기간, 부서 ID 중복, 운영 방식 활성, PM 액터 필수와 부서 유형별 액터, 단계 유무에 따른 기능, 멤버, 게이트 승인자. 이어서 requireAssignableDepartments(없거나 전부 활성 부서)와 requireInvitableUsers.
1. existsByCode가 참이면 409 DUPLICATE_PROJECT_CODE. 아니면 saveAndFlushNewProject. 저장 직전 경합의 UNIQUE 위반도 같은 409로 바꾼다.
1. 프로젝트 행 저장 뒤에 액터·주관 부서·기능·멤버(생성자가 첫 PM)를 저장한다. 단계 행은 그 다음 phaseRepository.saveAll(phasesOf)로 넣는다. 단계 이름 결정(applyTemplatePhases)과 이 DB 저장은 별개다.
1. defaultDataInitializers.initialize로 선택한 기능의 기본 데이터를 넣는다(KANBAN 기본 컬럼 등). 단계 행 저장은 바로 앞 단계에서 이미 끝난다.
1. 초기화와 PROJECT_CREATED 감사는 프로젝트·단계 저장과 같은 트랜잭션이다. 실패하면 함께 ROLLBACK된다.
1. CreationService → PostgreSQL: PROJECT_CREATED 감사 기록 · 같은 트랜잭션 COMMIT
1. FE / Controller → CreationService: 커밋 뒤 QueryService로 상세 재조회
1. CreationService → FE / Controller: 200 상세: departments[]·pms[]·features (복수 PM 추가는 역할 변경)
1. 오류면 생성·구조·감사 전부 ROLLBACK. code 재전송은 409 DUPLICATE_PROJECT_CODE.

## 03-2. 프로젝트 조회 · 단계와 가중 진척률
API: GET /api/projects · GET /api/projects/{projectId} · GET /api/projects/{projectId}/phases
참여자: FE, ProjectController, ProjectQueryService, ProjectAccessPolicy, ProgressCalculator, PostgreSQL
### 핵심 포인트
- 목록과 상세는 볼 수 없는 프로젝트를 없는 프로젝트와 구분하지 않는다. 상세는 404 PROJECT_NOT_FOUND. 시스템 관리자는 /api/projects/** 인가에서 404 NOT_FOUND.
- 단계 조회는 운영 방식과 무관하다. getVisibleProject만 통과하면 단계를 읽고, 단계가 없으면 오류 없이 200과 빈 목록이다.
- 상세의 프로젝트 진척률과 단계별 진척률은 말단 작업(다른 작업의 부모가 아닌 작업)의 가중 평균이다. 가중치는 estimatedEffort이고, 없으면 1이다. 대상이 없으면 0이며 반올림은 HALF_UP이다.

### 판정 순서
1. 목록 GET /api/projects: 뷰어를 읽고, 경영진은 소속 범위 쿼리, 그 외는 일반 사용자 쿼리. 최신 id 내림차순 커서. 목록 항목에는 가중 진척률을 넣지 않는다.
1. 상세 GET /api/projects/{projectId}: getVisibleProject. 없으면 404 PROJECT_NOT_FOUND.
1. 액터·기능·주관 부서·PM 멤버를 읽고, leafTasks의 ProgressCalculator.weightedAverage를 프로젝트 진척률로 담아 200.
1. 단계 GET /api/projects/{projectId}/phases: 다시 getVisibleProject. 운영 방식 값으로 막지 않는다.
1. phaseOrder 순으로 단계를 읽는다. 단계가 없으면 200과 빈 목록.
1. 말단 작업 중 phaseId가 있는 것만 단계별로 모아, 단계마다 weightedAverage를 계산해 같은 200 응답에 넣는다.

그림은 이번 작업에서 만들지 않았다. draw.io와 PNG는 사용자가 첨부한다.
## 03-3. 멤버 초대 · 전부 아니면 전무
API: POST /api/projects/{projectId}/members
참여자: PM FE / Controller, MemberService, Access / MemberPolicy, PostgreSQL
### 핵심 포인트
그림 원본: docs/sequence/03-3-member-invite.drawio / 내보낸 PNG: 03-3-member-invite.png. 그림은 문서 원본 명칭을 기준으로 안내하며 현재 develop에 해당 원본·PNG의 존재가 확인되지 않았다. 파일 첨부는 사용자 진행 예정이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. PM FE / Controller → MemberService: 요청 목록 1~100명 검증 후 invite(actor, projectId, members)
1. MemberService → Access / MemberPolicy: 공개 범위 404 → 현재 PM 여부 403
1. MemberService → PostgreSQL: 이미 멤버인 사용자·프로젝트 지정 액터 조회
1. MemberService → Access / MemberPolicy: 초대로 PM 지정 불가·미지정 액터·요청 중복 거부
1. MemberService → PostgreSQL: 모든 초대 대상 활성 USER 확인
1. MemberService → PostgreSQL: saveAllAndFlush: UNIQUE(project_id,user_id) 충돌은 409
1. MemberService → PostgreSQL: INVITED 이력 + PROJECT_MEMBER_INVITED 감사 INSERT
1. PostgreSQL → MemberService: COMMIT (어느 단계든 실패하면 전체 ROLLBACK)
1. MemberService → PM FE / Controller: 200 초대 멤버 목록 (userId 순). 재전송은 409
1. 초대는 프로젝트 행 잠금을 잡는 흐름이 아님. DB 유일 제약으로 동시 중복 초대를 막음. PM 추가는 초대 후 03-4 역할 변경.

## 03-4. 복수 PM 역할 변경·제외
API: PATCH members/{userId}/role · DELETE members/{userId}
참여자: PM FE / Controller, MemberService, Policy / Handler, PostgreSQL
### 핵심 포인트
PM 해제 정책은 지정 액터 검사 → 최소 PM 1명 보장 순서다. PM 추가는 활성 대상 검증 → 게이트 승인자 거부 순서다. 현재 역할과 요청 역할이 같으면 정책·version 변경 없이 성공한다. PM 추가/해제는 각각 PM_ADDED/PM_RELEASED 이력과 PROJECT_PM_ADDED/PROJECT_PM_RELEASED 감사다.
그림 원본: docs/sequence/03-4-member-role-change.drawio / 내보낸 PNG: 03-4-member-role-change.png. 그림은 문서 원본 명칭을 기준으로 안내하며 현재 develop에 해당 원본·PNG의 존재가 확인되지 않았다. 파일 첨부는 사용자 진행 예정이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. PM FE / Controller → MemberService: 역할 변경 또는 멤버 제외 요청
1. MemberService → PostgreSQL: projects 행 FOR UPDATE → 공개 범위/현재 PM 재판정
1. MemberService → PostgreSQL: 대상 멤버 조회 (없으면 404)
1. 역할 변경: 같은 역할이면 200, 변경·이력 없음. PM→다른 지정 액터는 마지막 PM이면 409 LAST_PM_REQUIRED.
1. MemberService → Policy / Handler: 추가 PM 대상 활성·게이트 승인자 아님 확인 / 지정 액터 검사
1. MemberService → PostgreSQL: 역할 UPDATE + PM_ADDED/PM_RELEASED/ROLE_CHANGED 이력·감사
1. 제외 분기: 마지막 PM → 게이트 승인자 → 미완료 담당 업무 검사 (미완료 담당 업무가 있으면 409 MEMBER_HAS_ASSIGNED_TASKS 및 업무 목록 반환; 기능 비활성 여부와 무관). 복수 PM 중 한 명은 조건 충족 시 제외 가능.
1. MemberService → PostgreSQL: 제외 가능하면 DELETE member + REMOVED 이력·감사
1. PostgreSQL → MemberService: COMMIT · 같은 프로젝트의 PM 변경을 직렬화
1. MemberService → PM FE / Controller: 200 역할 결과 또는 제외 response:null
1. 잠금·낙관적 충돌은 409 CONCURRENT_MODIFICATION. 실패하면 역할/삭제·이력·감사 함께 ROLLBACK.

## 03-6. 활성 PM 부재 시 관리자 복구
API: PUT /api/admin/projects/pm
참여자: 관리자 FE / API, ProjectPmService, MemberPolicy / Audit, PostgreSQL
### 핵심 포인트
활성 PM이 단 한 명이라도 남으면 복구 불가다. PM_REASSIGNED 이력 수는 항상 2행이 아니라 신규 PM 1행 + 제외된 비활성 PM마다 1행이다. 감사에는 newPmUserId, removedPmUserIds, addedAsMember가 남고 응답은 projectCode·pms[]다.
그림 원본: docs/sequence/03-6-admin-pm-recovery.drawio / 내보낸 PNG: 03-6-admin-pm-recovery.png. 그림은 문서 원본 명칭을 기준으로 안내하며 현재 develop에 해당 원본·PNG의 존재가 확인되지 않았다. 파일 첨부는 사용자 진행 예정이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. 관리자 FE / API → ProjectPmService: SYSTEM_ADMIN 인증·코드 정규화·새 PM ID 검증
1. ProjectPmService → PostgreSQL: projectCode로 projects FOR UPDATE · 기존 PM 전체 조회
1. ProjectPmService → PostgreSQL: PM별 현재 활성 상태 일괄 확인
1. 코드 없음 또는 활성 PM 한 명 이상이면 동일 409 PM_REASSIGN_NOT_ALLOWED (존재 비노출).
1. ProjectPmService → MemberPolicy / Audit: 새 PM은 활성 USER · 게이트 승인자 아님
1. ProjectPmService → PostgreSQL: 기존 멤버면 PM 전환, 비멤버면 PM으로 추가
1. ProjectPmService → PostgreSQL: 기존 비활성 PM 전원 제외 (새 PM 제외)
1. ProjectPmService → MemberPolicy / Audit: 신규/제외 PM별 PM_REASSIGNED 이력 + 감사
1. MemberPolicy / Audit → PostgreSQL: 같은 트랜잭션 COMMIT · 관리자 일반 프로젝트 조회 권한 없음
1. ProjectPmService → 관리자 FE / API: 200 projectCode·pms[] (전체 프로젝트 상세 없음)
1. 재전송은 새 PM이 활성이라 409. C안 일반 제외 검사와 다른 복구 예외이며 업무 자동 재배정·관리자 업무 목록 공개는 하지 않음.

## 03-8. 사용자 정의 단계 관리 · PM 잠금과 순서 재배치
API: POST /api/projects/{projectId}/phases · PATCH /api/projects/{projectId}/phases/{phaseId} · PUT /api/projects/{projectId}/phases/order · DELETE /api/projects/{projectId}/phases/{phaseId}. 참여자: PM FE, ProjectController, ProjectPhaseService, ProjectAccessPolicy, ProjectPhaseRepository, WbsTaskRepository, AuditLogger, PostgreSQL.
### 핵심 포인트
- 프로젝트 행을 FOR UPDATE로 잠그고 현재 PM 권한을 검증한다. 단계는 운영 방식과 관계없이 관리한다.
- 생성은 잠금 후 단계 수가 20 이상이면 409 PROJECT_PHASE_LIMIT_EXCEEDED. 순서 변경은 요청 ID가 현재 단계 집합과 정확히 같은지 검사하고, 아니면 400 INVALID_PROJECT_PHASE_ORDER. 임시 순서(size+index+1)를 flush한 뒤 최종 순서(index+1)를 flush한다.
- 이름 변경은 저장 이름과 strip한 요청 이름이 같으면 감사 없이 반환한다. 삭제는 그 단계에 WBS 업무가 있으면 409 PROJECT_PHASE_IN_USE. 없으면 삭제 후 flush하고, 남은 단계에 임시 순서(현재 최대 순서+index+1)를 flush한 다음 최종 순서(index+1)를 flush하고 PROJECT_PHASE_DELETED 감사를 같은 트랜잭션에 남긴다.

### 판정 순서
1. FE → Controller: 단계 생성·이름 변경·전체 순서 변경·삭제 요청
1. Controller → ProjectPhaseService: @Transactional BEGIN
1. ProjectPhaseService → ProjectAccessPolicy → PostgreSQL: 프로젝트 행 잠금, 공개 범위·PM 권한 검사
1. ProjectPhaseService → Repository: 단계·이름·기존 순서 조회 및 입력 유효성 검사
1. alt 순서 변경: 요청 ID가 전체 단계와 동일한 집합인지 검사 → 임시 phaseOrder로 변경 → flush → 최종 순서로 변경 → flush
1. alt 삭제: existsByPhaseId이면 409 PROJECT_PHASE_IN_USE와 ROLLBACK. 없으면 delete → flush → 임시 순서 → flush → 최종 순서 → flush → PROJECT_PHASE_DELETED
1. alt 생성: count가 20 이상이면 409 PROJECT_PHASE_LIMIT_EXCEEDED. 아니면 순서 count+1로 저장하고 PROJECT_PHASE_CREATED. alt 이름 변경: 이름이 같으면 감사 없이 200. 다르면 rename 후 PROJECT_PHASE_RENAMED
1. ProjectPhaseService → AuditLogger: 변경 이력 같은 트랜잭션 기록
1. PostgreSQL → Controller: COMMIT, 생성은 200 ProjectPhaseResponse, 이름·순서 변경과 삭제는 200 response:null 반환

그림 작성 기준: 기존 03 시퀀스와 같은 참여자 레인, 왼쪽에서 오른쪽으로 호출 화살표, 분기·트랜잭션 조건은 노란 상자. draw.io/PNG는 사용자가 첨부 예정이며 현재 원격 파일 존재를 확인하지 않았다.
## 03-9. 프로젝트 설정 변경 · version·설정 이력
API: `PATCH /api/projects/{projectId}/settings · GET /api/projects/{projectId}/setting-histories`
참여자: PM FE / ProjectSettingsController / ProjectSettingsService / ProjectAccessPolicy / 관련 Repository / AuditLogger / PostgreSQL
### 핵심 포인트
- 설정 PATCH는 변경 유형을 최소 하나 이상 요구한다. 프로젝트 행 FOR UPDATE 후 PM 권한 및 요청 version을 검증한다.
- 기본 정보·방법론·액터·기능·게이트 승인자를 각각 조건 검증한다. 실제 변경이 없으면 수정 이력이나 version 증가를 만들지 않는다.
- 실제 변경 시 이력 저장 및 PROJECT_SETTINGS_CHANGED 감사, OPTIMISTIC_FORCE_INCREMENT로 version 증가를 같은 트랜잭션에 묶는다. 이력 GET은 공개 범위 내 읽기 권한, id 내림차순 커서.
- 업무 존재 시 운영 방식 변경 제한, 제거할 액터의 기존 멤버 사용 여부 등 각 변경 유형별 검사를 선행한다.

### 판정 순서
1. PM FE → Controller → Service: PATCH 요청 및 version
1. 메서드 진입으로 트랜잭션이 시작된 뒤, 잠금 전에 변경 유형이 하나도 없으면 400 INVALID_PROJECT_SETTINGS. 그 다음 getManagedProjectForUpdate: 공개 범위가 아니면 404 PROJECT_NOT_FOUND, PM이 아니면 403 PROJECT_PM_ONLY, projects FOR UPDATE.
1. Service → DB: 현재 project.version과 요청 version 비교(다르면 409 CONCURRENT_MODIFICATION)
1. alt basicInfo/methodology/actors/features/gateApprover: 각 유형의 현재 데이터·연결된 업무/멤버를 검증하고 변경 전후 설정 이력 저장
1. 각 change*는 값이 같으면 false이며 설정 이력을 쓰지 않는다. 바뀐 유형이 하나도 없으면 version을 올리지 않고 PROJECT_SETTINGS_CHANGED 감사도 없이 반환한다.
1. else 하나라도 바뀌면 OPTIMISTIC_FORCE_INCREMENT로 version을 올리고, changeTypes를 담은 PROJECT_SETTINGS_CHANGED 감사 한 건을 같은 트랜잭션에 남긴다. 실패하면 설정·이력·감사가 함께 ROLLBACK.
1. PostgreSQL → Controller: COMMIT 후 프로젝트 상세 재조회 반환
1. FE → Controller: GET setting-histories(cursor,size) → 공개 범위 판정 → 이력·변경자 일괄 조회 → 200 CursorPage

draw.io 작성 기준: 기존 프로젝트 Sequence의 레인 및 화살표 방향, 분기·동시성·트랜잭션은 노란 조건 상자. 원본 및 PNG는 사용자 첨부 예정(본문 삽입 미완료).
## 변경 이력
November 22, 2026 — 현재 구현 대조: 복수 PM·복수 부서/생성 권한 개방·폐지 위임·관리자 복구, WBS 상한/토큰/잠금, COUNT WIP, TODO 멱등/소프트 삭제/raw JPA409 중 해당 흐름 정정. 모든 그림은 draw.io와 PNG를 동일 좌표 모델에서 생성. 새 첨부는 진행 상태에 따라 별도 확인한다.
| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| November 15, 2026 | 최초 작성 (흐름 2개) | SCRUM-98, 커밋 9940a75·8ae3cd4 |
| November 17, 2026 | 03-2에 멤버·이력 조회와 `ProjectAccessPolicy` 반영(텍스트만), FR-PRJ-04 흐름 4개(03-3~03-6) 추가. 그림 원본 `docs/sequence/03-3~03-6` | SCRUM-98, SCRUM-106, 커밋 5abd50c. 2026-11-27 develop 50f96d2076b09500c986c684d0468f430a00601e: 03-1을 권한 → applyTemplatePhases → 정책 → 부서·멤버 → 프로젝트 저장과 단계 저장 순서로 고치고 03-2 조회를 복구. 03-8에 최대 20개, 동일 이름 감사 생략, 삭제 후 임시 순서 flush를 반영. 03-9는 잠금 전 변경 유형 검사와 무변경 시 version·감사 생략을 코드 순서로 확인. |
