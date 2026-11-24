# CoreviaAI_BE

> 상태: Draft
> 담당자: 미정 (팀에서 지정 필요)
> 마지막 검토일: 2026-11-22
> 적용 범위: `CoreviaAI_BE/` 백엔드 (Spring Boot)

## 1. 프로젝트 개요

- 개발 규칙: [공통 코드·다국어·테마 적용과 PR 완료 기준](docs/development/code-i18n-theme-rules.md) · [Confluence 규칙](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/21397670). Codex·Claude·Cursor 지침과 PR 체크리스트에 같은 기준을 적용한다(SCRUM-171 / NFR-20).

- 목적: CoreviaAI(원익큐앤씨 전사 AI 협업관리 웹앱)의 백엔드 API. 요구사항 기준은 Confluence [요구사항명세 최종안 : 전사관리](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909)다.
- 구현된 기능
  - FR-ADM-01 로그인·계정 (Jira SCRUM-96): ID/PW 로그인·로그아웃, 토큰 재발급, 내 계정 조회, 비밀번호 변경, 관리자 비밀번호 초기화, 5회 연속 실패 잠금, 30분 무활동 세션 만료, 로그인 시도 기록
  - FR-ADM-02 사용자·조직 관리 (Jira SCRUM-97): 사용자 추가·수정·활성/비활성, 시스템 역할 변경, 부서(계층) 관리, 변경 이력(감사 로그) 조회
  - FR-PRJ-01 프로젝트 생성 (Jira SCRUM-98): 생성 권한 제한 없음(활성 일반 사용자 전원), 단계형 생성(기본 정보 → 템플릿·운영 방식 → 사용자 정의 단계 → 액터 → 기능 구성 → 멤버(선택) → 게이트 승인자(선택)), 기간·주관 부서(0~N개) 후입력, 생성자 PM 자동 지정, 템플릿 기본 단계 또는 사용자 지정 단계 생성, 공개 범위에 따른 목록(필터·커서)·상세·멤버 목록(커서) 조회. MVP는 방법론 없음·폭포수, SW 액터만 선택 가능
  - FR-PRJ-06·09 기능 구성·템플릿(일부): 생성 시 기능 모듈 선택(Git 연동 비활성), 빈/IT 프로젝트 템플릿 기본값 제공. 관리자 템플릿 관리 화면과 설정 변경(FR-PRJ-14)은 후속
  - FR-ADM-07 공통 코드 관리 (Jira SCRUM-168): 코드 그룹(위험 수준·업무 진행 상태·요청 처리 결과·프로젝트 상태)과 코드 테이블, 관리자 정렬 순서 변경(코드 값은 고정: 추가·수정·삭제와 사용 여부 변경은 마이그레이션으로만, 감사 로그), 화면용 그룹별 코드 조회
  - FR-ADM-06 다국어 라벨 관리 (Jira SCRUM-169): 한국어·영어·번체 중국어 라벨 테이블(`i18n_labels`, 라벨 키 + 언어별 컬럼), 화면용 언어별 라벨 묶음 조회(로그인 전 공개, 번역 누락은 한국어), 관리자 라벨 등록·문구 수정(낙관적 잠금)·키/문구 검색·번역 누락 필터, 문구 변경 이력(감사 로그), 사용자별 표시 언어 저장·조회 및 저장 언어 기준 라벨 조회. 해외법인 기본값은 별도 법인 모델 확정 전 범위에서 제외
  - FR-ADM-09 에러 로그 관리 (Jira SCRUM-170) + NFR-06 패턴 마스킹 일부 (Jira SCRUM-114): 전역 예외 처리기에 도달한 미처리 예외(500)를 `error_logs`에 별도 트랜잭션으로 기록(발생 시각·메서드·경로·사용자 ID·예외 클래스·마스킹한 메시지·원인 포함 스택), 사용자에게는 공통 500만 응답, 시스템 관리자 조회(기간·유형·경로 필터, 커서, 상세), UTC 기준 1년 초과 로그의 시간별 배치 정리
  - FR-WBS-01 계층형 WBS (Jira SCRUM-107): 작업 > 하위작업 2단계 트리 조회(한 번에, 담당자 이름 포함), 작업·하위작업 등록·수정(낙관적 잠금)·삭제(연결 있으면 거부)·드래그 이동(순서·계층 변경), 상위 작업 기간을 하위작업 기간으로 자동 산출(저장값 갱신). 권한은 3.3 WBS-01(PM 전체, 개발자·QA 본인 담당 작업 수정·그 아래 하위작업 추가). '단계' 레벨 표현은 미결
  - FR-WBS-03 선후행 관계 (Jira SCRUM-118): 완료→시작 선행 작업 목록 조회·전체 교체·해제, 같은 프로젝트 작업만 허용하고 자기 참조·간접 순환을 차단. 선행 작업이 종료일을 넘긴 채 미완료이면 한국 날짜 기준 지연 일수와 후행 영향 여부를 계산. 권한은 PM 전체, 개발자·QA 본인 담당 후행 작업 변경, 프로젝트 열람 가능자는 조회
  - FR-WBS-04 마일스톤 (Jira SCRUM-108): 마일스톤 목록(목표일 순)·상세, 등록(출처 MANUAL)·이름·목표일 수정(낙관적 잠금)·삭제(연결 정보 함께 삭제, 감사 로그), 연결 작업 전체 교체(같은 프로젝트 작업만). D-day와 지연 경고(목표일 당일부터, 연결 작업 중 미완료가 있으면)를 조회 때 한국 시간 기준으로 계산(저장 안 함). 권한은 3.3 WBS-04(PM C·U·D, 그 밖 조회). 알림(FR-NTF-01)은 Sprint 3
  - FR-TSK-01 업무 CRUD (Jira SCRUM-109): 업무 = WBS 작업·하위작업. 업무 상세 조회, 유형(TASK·DEFECT)·우선순위(HIGH·MEDIUM·LOW)·완료 조건 수정(낙관적 잠금, WBS 등록 때 선택 입력 가능), 변경 이력(`task_histories`: 등록·배정·해제·일정·상세·이동·삭제, 업무별 시간순 커서 조회). 등록·담당자 배정·기간·삭제는 FR-WBS-01 API를 그대로 쓴다. 담당자 TODO와 개인 통합 일정은 `assignee_user_id` 원본을 조회하므로 배정·재배정·기간·상태 변경이 복제 없이 즉시 반영
  - FR-PRJ-04 멤버·역할 관리 (Jira SCRUM-106): 멤버 초대·역할 변경·제외(PM만), 복수 PM(역할 변경으로 추가·해제, 최소 1명), 멤버 이력, PM 재지정(활성 PM이 없을 때 시스템 관리자). 담당 업무 수는 완료 여부와 관계없이 해당 프로젝트의 담당 업무·하위 업무를 집계하며, 미완료 담당 업무가 있으면 멤버를 제외할 수 없다.
- 기술 stack: Java 17, Spring Boot 4.1.2 (Web MVC, Security, Data JPA, Data Redis, Validation, Flyway), jjwt 0.13, PostgreSQL, Redis 7.4

## 2. 개발 환경

| 항목 | 요구사항 |
|---|---|
| JDK | 17 |
| Build | Gradle Wrapper 9.8.2 (저장소에 포함) |
| DB | PostgreSQL (운영은 회사 제공 DB, NFR-16). 버전은 회사 DB 확정 후 기록 |
| 세션 저장소 | Redis 7.4 (`compose.yaml`로 함께 기동) |
| Container | Docker, Docker Compose v2 |
| 테스트 | 외부 서비스 불필요 (H2 PostgreSQL 호환 모드 + 메모리 세션 저장소) |

## 3. 설치·실행·검증

Windows는 `gradlew.bat`을 쓴다.

```
./gradlew test             # 단위 테스트(src/test/unit). 성공 기준: BUILD SUCCESSFUL, 실패 0건
./gradlew integrationTest  # 통합 테스트(src/test/integration). 성공 기준 같음
./gradlew check            # 단위 + 통합 + 커버리지 리포트(CI와 같은 기준)
./gradlew bootJar    # 성공 기준: build/libs/CoreviaAI_BE-0.0.1-SNAPSHOT.jar 생성
```

Docker Compose로 실행 (BE + Redis):

```
cp .env.example .env   # 값 채우기 (아래 4절)
docker compose up -d --build
docker compose logs -f corevia-be   # 성공 기준: "Started CoreviaAiBeApplication" 로그
```

로컬에서 `bootRun`으로 실행할 때는 Redis만 띄우고 환경변수를 셸에 설정한다.

```
docker compose up -d redis
./gradlew bootRun
```

- format·lint: 아직 도구가 없다(checkstyle·spotless 미도입). CI 구성 시 추가 필요.
- DB 스키마는 기동 시 Flyway가 적용하고, Hibernate는 `ddl-auto=validate`로 엔티티와 스키마가 맞는지만 확인한다.

## 4. 설정과 환경

| 환경변수 | 필수 | 의미 | 예시 형식 |
|---|---|---|---|
| `DB_URL` | 필수 | PostgreSQL JDBC URL | `jdbc:postgresql://<host>:5432/<db>` |
| `DB_USERNAME` / `DB_PASSWORD` | 필수 | DB 계정 | |
| `JWT_SECRET` | 필수 | JWT 서명 키. Base64, 디코딩 후 32바이트 이상 | `openssl rand -base64 48` 결과 |
| `AUTH_COOKIE_SECURE` | 선택(기본 true) | 쿠키 Secure 속성. HTTPS가 아닌 로컬에서만 false | `true` |
| `REDIS_HOST` / `REDIS_PORT` | 선택(기본 localhost/6379) | Redis 주소. compose에서는 자동 설정 | |
| `BE_PORT` | 선택(기본 8080) | compose가 호스트에 여는 포트 | |
| `INITIAL_ADMIN_LOGIN_ID` / `_PASSWORD` / `_NAME` | 최초 1회 | 시스템 관리자가 없을 때 첫 관리자 생성. 생성 후 비워 둔다 | 비밀번호는 영문·숫자·특수문자 포함 8자 이상 |

- 로컬은 `.env`(커밋 금지, `.gitignore` 등록)로 주입한다. 저장소에는 값이 없는 `.env.example`만 둔다.
- 테스트 설정은 `src/test/integration/resources/application-test.properties` (테스트 전용 키, 운영 비밀값 아님).
- 운영 secret 관리·rotation 책임: 미정. 배포 가이드(NFR-16) 수령 후 결정 필요. `JWT_SECRET`을 바꾸면 발급된 모든 토큰이 무효가 되어 전원 재로그인한다.

### 최초 관리자 계정

비밀번호를 코드·SQL에 남기지 않기 위해, 시스템 관리자가 한 명도 없을 때만 `INITIAL_ADMIN_*` 값으로 첫 관리자를 만든다. 이 계정은 첫 로그인 후 비밀번호를 바꿔야 다른 기능을 쓸 수 있다. 관리자가 이미 있으면 값이 남아 있어도 무시한다.

## 5. 구조와 Architecture

WebUI_BE의 `authserver / erpweb / global` 구성을 따른다. 인증 서버(`authserver`)는 이후 사내 SSO로 교체될 수 있으므로 업무 모듈과 분리한다. AI 기능(`aiserver`)은 별도 FastAPI 서버(CoreviaAI_AI) 코드를 Java로 옮겨 이 애플리케이션 안의 모듈로 둔다.

```
org.wonik
├─ CoreviaAiBeApplication
├─ authserver/                       인증 서버 (로컬 ID/PW). 사내 SSO 도입 시 교체 대상
│  ├─ domain/auth
│  │  ├─ controller                  AuthController, MyPasswordController, AdminPasswordController
│  │  ├─ authenticator               CredentialAuthenticator(인터페이스) + LocalCredentialAuthenticator
│  │  ├─ service                     AuthService(로그인·재발급·로그아웃), PasswordService
│  │  ├─ session                     AuthSessionStore(인터페이스) + RedisAuthSessionStore, AuthSessionProperties
│  │  ├─ policy                      LoginLockPolicy(5회), PasswordPolicy
│  │  ├─ loginhistory                로그인 시도 기록 (append-only)
│  │  ├─ bootstrap                   최초 관리자 생성
│  │  └─ dto
│  ├─ domain/credential              LocalCredential(local_credentials): 비밀번호 해시·실패 횟수·잠금·변경 강제
│  └─ security                       AuthServerSecurityConfig, AuthSessionLoginResolver, AuthSessionTerminator
├─ aiserver/                         AI 모듈. FastAPI(CoreviaAI_AI) 코드를 Java로 옮기는 중. 업무 모듈을 import하지 않는다
│  └─ domain/wbs
│     ├─ api                         업무 모듈에 공개하는 계약: WbsDraftGenerator + 요청·응답 record(AiWbs*, 02. AI 생성 API 형태)
│     └─ service                     NotReadyWbsDraftGenerator: 이식 전 임시 구현(항상 503 AI_NOT_READY)
├─ coreviaai_be/                     업무 모듈
│  ├─ domain/department              Department(departments): 부서 계층(parentId). 사용자 소속·이후 프로젝트 주관 부서의 기준
│  ├─ common/web                     CoreviaExceptionHandler: 업무 모듈 전체의 동시 수정 충돌 → 409 (authserver 응답은 그대로)
│  ├─ domain/project                 프로젝트 (FR-PRJ-01, FR-PRJ-04)
│  │  ├─ controller                  ProjectController(생성·목록·상세·단계), ProjectMemberController(멤버·이력·PM 추가·해제)
│  │  ├─ service                     ProjectCreationService(생성), ProjectQueryService(목록·상세·단계), ProjectMemberService(멤버·이력·복수 PM), ProjectPmService(관리자 PM 재지정)
│  │  ├─ policy                      ProjectCreationPolicy(생성 규칙), ProjectMemberPolicy(멤버·역할·PM 최소 인원 규칙), ProjectAccessPolicy(조회 404·PM 전용 403), ProjectVisibilityPolicy(공개 범위·경영진 소속 범위)
│  │  ├─ service                     ProjectCreationService(템플릿 기본값 또는 요청 단계 생성) + ProjectPhaseService(단계 CRUD·순서)
│  │  ├─ entity                      Project, ProjectActorAssignment, ProjectMember, ProjectMemberHistory, ProjectPhase, type/(enum)
│  │  └─ dto                         request / response
│  ├─ domain/code                    공통 코드(FR-ADM-07, code_groups·codes): CodeGroup, CommonCode, 화면용 조회 CodeController(/api/codes/{groupCode})·CodeQueryService
│  ├─ domain/i18n                    다국어 라벨(FR-ADM-06, i18n_labels): I18nLabel, LabelLanguage(표시 언어 결정), 화면용 묶음 LabelController(/api/i18n/labels)·LabelBundleService
│  ├─ domain/aidraft                 AI 초안·추정 기록(ai_drafts): AiDraft, AiDraftRecorder. 저장만 담당하고 AI 기능 로직은 각 도메인에 둔다
│  ├─ domain/wbs                     WBS·일정·업무 (FR-WBS-01 계층형 WBS, FR-WBS-04 마일스톤, FR-PRJ-07 AI 초안 생성·반영, FR-TSK-01 업무 상세·변경 이력)
│  │  ├─ controller                  WbsTaskController(트리 조회·등록·수정·이동·삭제), TaskController(업무 상세·상세 필드 수정·변경 이력), MilestoneController, WbsAiDraftController(초안 생성·승인 반영)
│  │  ├─ service                     WbsTaskService(직접 편집·상위 기간 산출), TaskDetailService(업무 상세), TaskHistoryRecorder(변경 이력 기록, 모든 쓰기 경로가 사용), WbsTreeReader(트리 조립), WbsDraftGenerationService(AI 호출·토큰 발급), WbsDraftApplyService(토큰 소비·반영), WbsDraftContextReader, WbsItemWriter
│  │  ├─ policy                      WbsTaskAccessPolicy(공개 범위·WBS 기능·프로젝트 행 잠금), WbsEditor(역할별 변경 권한), WbsTaskRules(기간·담당자), WbsDraftAccessPolicy(PM·단계 존재·WBS 기능), WbsDraftValidator(참조·기간·순환·담당자)
│  │  ├─ client                      WbsDraftAiClient: 도메인 입력·결과 ↔ aiserver 계약(`aiserver.domain.wbs.api`) 변환
│  │  ├─ drafttoken                  WbsDraftTokenStore(인터페이스) + RedisWbsDraftTokenStore: 1회용 draftToken
│  │  ├─ entity                      WbsTask, TaskHistory, WbsDependency, Milestone, MilestoneTask, TaskDetails(기본 필드), TaskAttributes(상세 필드), TaskSnapshot(이력용 전·후 값), type/(enum)
│  │  └─ dto                         request / response (service/command·result는 서비스 입출력)
│  ├─ domain/admin                   관리자 콘솔 (FR-ADM-02, FR-ADM-06, FR-ADM-07, FR-ADM-09)
│  │  ├─ controller                  AdminUserController, AdminDepartmentController, AdminProjectController(PM 재지정), AdminCodeController(공통 코드), AdminLabelController(다국어 라벨), AdminErrorLogController(에러 로그 조회)
│  │  ├─ service                     AdminUserService, AdminDepartmentService, AdminCodeService, AdminLabelService, AdminErrorLogService
│  │  │  ├─ command                  서비스 입력 (요청 DTO와 분리)
│  │  │  └─ result                   서비스 결과 (응답 DTO는 컨트롤러에서 변환)
│  │  └─ dto                         request / response
│  └─ security                       CoreviaSecurityConfig
└─ global/                           공통. authserver·coreviaai_be를 import하지 않는다
   ├─ user                           User(users): 사람·조직 정보(소속 부서·직무 포함). authserver·업무 모듈이 함께 사용
   ├─ audit                          AuditLog(audit_logs), AuditLogger, AuditActor: 감사 로그. 변경과 같은 트랜잭션에서 기록
   ├─ errorlog                       ErrorLog(error_logs), ErrorLogRecorder(REQUIRES_NEW 기록), StackTraceFormatter: 미처리 예외 에러 로그
   ├─ jwt                            JwtTokenProvider, AuthCookieManager, JwtProperties, LoginUser(principal)
   ├─ security                       JwtAuthenticationFilter, LoginSessionResolver·UserSessionTerminator(인터페이스), ApiSecuritySupport
   ├─ common/type                    SystemRole
   ├─ common/entity                  BaseTimeEntity (createdAt·updatedAt, UTC Instant)
   ├─ common/exception               ErrorCode, CustomException, GlobalExceptionHandler(최종 Exception 처리 → 공통 500 + 에러 로그)
   ├─ common/masking                 SensitiveValueMasker: 비밀 키 값·JWT·Bearer·DB 상세 값·이메일·전화·주민번호 패턴 마스킹(NFR-06)
   ├─ common/response                ApiResponse
   ├─ common/pagination              CursorCodec, CursorPageRequest, CursorPage (커서 목록)
   ├─ common/time                    BusinessCalendar: 업무 날짜 기준 시간대(Asia/Seoul)와 Clock 기준 '오늘'
   └─ config                         Clock, JPA auditing, PasswordEncoder, 401/403 JSON 응답, AuditActor 인자 주입(web)
```

- 의존 방향: `authserver → global`, `coreviaai_be → global`, `aiserver → global`, `coreviaai_be → aiserver.*.api`(공개 계약만). aiserver는 업무 모듈을, 업무 모듈은 aiserver 구현 패키지를 import하지 않는다. 기능별 AI 요청·응답 타입은 global이 아니라 aiserver 계약 패키지에 둔다. 업무 코드는 인증 결과를 `@AuthenticationPrincipal LoginUser`(global)로만 받고 `authserver`를 import하지 않는다. 그래서 SSO로 바뀌면 `authserver`만 교체하면 된다.
- 인증 방식 분리는 두 단계로 되어 있다: 패키지 단위(`authserver` 교체)와, 같은 서버 안에서 자격 증명 확인만 바꾸는 `CredentialAuthenticator` 구현체 교체.
- 보안 체인은 경로를 소유한 모듈이 갖는다(WebUI_BE ADR-0007 방식).
  - `@Order(1)` `authserver.security.AuthServerSecurityConfig`: `/api/auth/**`, `/api/users/me/password`, `/api/admin/users/*/password-reset`
  - `@Order(2)` `coreviaai_be.security.CoreviaSecurityConfig`: 나머지 `/api/**` (로그인 필수, `/api/admin/**`은 시스템 관리자, `/api/projects/**`은 일반 사용자·경영진만. 시스템 관리자는 프로젝트 데이터 접근 불가)
  - 마지막 `global.config.security.WebSecurityConfig`: 위에 속하지 않는 경로는 모두 거부(`/error`만 허용)
  - 공통 설정과 JWT 필터는 `global.security`에 있고, 세션 확인은 `LoginSessionResolver` 인터페이스로만 한다. 구현(`AuthSessionLoginResolver`, Redis 세션)은 authserver에 있어 SSO 교체 시 이것만 바꾸면 업무 체인은 그대로 쓴다.
- 사용자와 로그인 정보 분리: 사람·조직 정보(`users`, `global.user`)와 로컬 ID/PW 로그인 정보(`local_credentials`, `authserver.domain.credential`)를 나눴다. `LocalCredential`은 `User`와 JPA 연관관계 없이 `user_id`만 갖는다. SSO로 바꾸면 `local_credentials` 테이블과 `authserver`만 제거하고, `users`와 이를 참조하는 업무 데이터는 그대로 둔다. FR-ADM-02의 사용자 추가는 `users` 생성(업무 모듈) + 임시 비밀번호 발급(authserver 관리자 초기화 API) 두 단계가 된다.
- 관리자 기능의 계층: 컨트롤러는 요청 DTO를 command로 바꿔 서비스에 넘기고, 서비스 결과(result)를 응답 DTO로 바꾼다. 감사 로그 행위자(`AuditActor`)는 `AuditActorArgumentResolver`가 로그인 사용자와 접속 IP로 채운다.
- 세션 끊기: 관리자가 역할을 바꾸거나 계정을 비활성화하면 업무 모듈이 `UserSessionTerminator`(global)를 호출하고, authserver 구현(`AuthSessionTerminator`)이 Redis 세션을 지운다. 세션에 저장된 역할로 인가하므로 기존 세션을 끊어야 바뀐 권한이 즉시 적용된다. 업무 모듈은 authserver를 import하지 않는다.
- ADR: 아직 없음. 세션+JWT 인증 방식과 이 패키지 구성은 ADR로 남길 대상이다(팀 결정 필요).

## 6. API

- API 명세 위치: Confluence CoreviaAI [API 명세서](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/folder/12877843) (공통 규약은 `00. 공통 규약`, 관리 API는 `BE_API/02. 관리`). 이 절은 요약이다.
- 응답 형식: `{"success": bool, "response": ..., "error": {"message": "...", "status": 400}}`

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | `{loginId, password}` → 쿠키 발급, 계정 정보 응답 |
| POST | `/api/auth/refresh` | RefreshToken 쿠키 | AccessToken 재발급 |
| POST | `/api/auth/logout` | 불필요(있으면 세션 종료) | 세션 삭제, 쿠키 만료 |
| GET | `/api/auth/me` | 필요 | 내 계정 정보 (`mustChangePassword` 포함) |
| PATCH | `/api/users/me/password` | 필요 | `{currentPassword, newPassword}` → 성공 시 모든 세션 종료, 재로그인 |
| POST | `/api/admin/users/{userId}/password-reset` | SYSTEM_ADMIN | 임시 비밀번호 발급(응답에 1회), 잠금 해제, 대상 사용자 세션 종료, 감사 로그(`PASSWORD_RESET`) 기록 |
| GET | `/api/admin/users` | SYSTEM_ADMIN | 사용자 목록(커서). `cursor`, `size`, `departmentId`, `status`, `systemRole`, `keyword` |
| GET | `/api/admin/users/{userId}` | SYSTEM_ADMIN | 사용자 상세 |
| POST | `/api/admin/users` | SYSTEM_ADMIN | 사용자 추가. 비밀번호는 이어서 password-reset으로 발급 |
| PATCH | `/api/admin/users/{userId}` | SYSTEM_ADMIN | 이름·부서·직무 수정(보낸 값으로 덮어씀) |
| PATCH | `/api/admin/users/{userId}/system-role` | SYSTEM_ADMIN | 역할 변경 + 대상 세션 종료 |
| PATCH | `/api/admin/users/{userId}/status` | SYSTEM_ADMIN | 활성·비활성. 비활성 시 대상 세션 종료 |
| GET | `/api/admin/users/{userId}/histories` | SYSTEM_ADMIN | 변경 이력(감사 로그, 커서) |
| GET | `/api/admin/departments` | SYSTEM_ADMIN | 부서 목록(커서, id 오름차순). `cursor`, `size`, `status` |
| POST | `/api/admin/departments` | SYSTEM_ADMIN | 부서 추가 |
| PATCH | `/api/admin/departments/{departmentId}` | SYSTEM_ADMIN | 이름 변경·상위 부서 이동 |
| PATCH | `/api/admin/departments/{departmentId}/status` | SYSTEM_ADMIN | 활성·비활성 |
| GET | `/api/admin/code-groups` | SYSTEM_ADMIN | 공통 코드 그룹 목록(id 오름차순, 페이지 없음) |
| GET | `/api/admin/code-groups/{groupCode}/codes` | SYSTEM_ADMIN | 그룹의 코드 전체(사용 안 함 포함, 정렬 순서) |
| PATCH | `/api/admin/code-groups/{groupCode}/codes/{code}` | SYSTEM_ADMIN | 정렬 순서(`sortOrder`) 변경. 코드 추가·수정·삭제와 사용 여부 변경 API는 없음(마이그레이션으로만). 감사 로그 `CODE_CHANGED` |
| GET | `/api/codes/{groupCode}` | 로그인 사용자 | 화면용 사용 중 코드 목록(코드·라벨 키·정렬 순서) |
| GET | `/api/admin/i18n/labels` | SYSTEM_ADMIN | 다국어 라벨 목록(커서, id 오름차순). `cursor`, `size`, `keyword`(라벨 키·한국어 문구 부분 일치), `missing`(`en`·`zh-TW` 번역 누락만) |
| POST | `/api/admin/i18n/labels` | SYSTEM_ADMIN | 라벨 등록(`labelKey`, `textKo` 필수, `textEn`·`textZhTw` 선택). 같은 키 409. 감사 로그 `I18N_LABEL_CREATED` |
| PATCH | `/api/admin/i18n/labels/{labelId}` | SYSTEM_ADMIN | 문구 수정(세 언어 덮어쓰기 + `version`, 다르면 409). 라벨 키 변경·삭제 API 없음. 바뀐 언어마다 감사 로그 `I18N_LABEL_CHANGED` |
| GET | `/api/admin/error-logs` | SYSTEM_ADMIN | 에러 로그 목록(커서, 최신순, 스택 제외). `cursor`, `size`, `from`(포함)·`to`(제외) ISO-8601 UTC, `errorType`(예외 클래스 정확히 일치), `apiPath`(경로 앞부분 일치). from ≥ to면 400 `INVALID_PERIOD` |
| GET | `/api/admin/error-logs/{errorLogId}` | SYSTEM_ADMIN | 에러 로그 상세(마스킹된 스택 트레이스 포함). 없으면 404 `ERROR_LOG_NOT_FOUND`. 수정·삭제 API 없음 |
| GET | `/api/i18n/labels` | 불필요(공개) | 화면용 라벨 묶음 `{lang, labels: {키: 문구}}`. 언어 `lang` → `Accept-Language` → ko, 번역 누락은 한국어. 비밀번호 변경 강제 세션도 허용 |
| GET·PUT | `/api/me/language` | 로그인 사용자 | 본인의 저장 언어(`ko`·`en`·`zh-TW`, null이면 ko) 조회·변경. PUT은 `version` 낙관적 잠금과 감사 로그를 사용 |
| GET | `/api/me/language/labels` | 로그인 사용자 | 저장된 사용자 언어로 라벨 묶음 조회. 응답은 `no-store` |
| GET | `/api/projects/creation-options` | USER·EXECUTIVE | 생성 화면 ②·③·④ 선택지(템플릿 기본값, 운영 방식, 부서 유형별 액터, 기능 모듈. `enabled`·`required`) |
| GET | `/api/projects/code-availability` | 활성 USER | `?code=` 코드 중복 확인(대문자로 정규화한 코드와 `available`) |
| POST | `/api/projects` | 활성 USER | 프로젝트 생성. `template`·`features`·`departmentIds`(0~N) 필수 형식, 기간·`phases` 선택. `phases` 생략 시 템플릿 기본값, 빈 배열이면 0개, 값이 있으면 사용자 정의 단계 생성. 생성자 PM 자동 지정, 감사 로그(`PROJECT_CREATED`). 응답은 상세 |
| GET | `/api/projects` | USER·EXECUTIVE | 프로젝트 목록(커서, id 내림차순). `cursor`, `size`, `status`, `departmentId`(주관 부서 중 하나), `pmUserId`(PM 중 한 명). 항목의 `departments`·`pms`는 배열. 공개 범위 밖은 제외 |
| GET | `/api/projects/{projectId}` | USER·EXECUTIVE | 상세(운영 방식과 무관한 공통 정보: 기본 정보·템플릿·운영 방식·주관 부서·PM 목록·지정 액터·기능 구성). 볼 수 없으면 404 |
| GET | `/api/projects/{projectId}/members` | USER·EXECUTIVE | 멤버 목록(커서, id 오름차순 = 등록순). `cursor`, `size`. 공개 범위는 상세와 같음. 항목에 해당 프로젝트의 완료 업무를 포함한 `assignedTaskCount` |
| POST | `/api/projects/{projectId}/members` | PM | 멤버 초대(한 번에 최대 100명). 이미 멤버가 있으면 전체 거부(409 `ALREADY_PROJECT_MEMBER`) |
| PATCH | `/api/projects/{projectId}/members/{userId}/role` | PM | 역할 변경. `PM`으로 바꾸면 PM 추가, PM을 다른 역할로 바꾸면 PM 해제(마지막 PM이면 409 `LAST_PM_REQUIRED`). 같은 역할이면 변경·이력 없음 |
| DELETE | `/api/projects/{projectId}/members/{userId}` | PM | 멤버 제외. 마지막 PM·게이트 승인자는 409 |
| GET | `/api/projects/{projectId}/members/histories` | USER·EXECUTIVE | 멤버 이력(커서, id 내림차순). 공개 범위는 상세와 같음 |
| PUT | `/api/admin/projects/pm` | SYSTEM_ADMIN | PM 재지정(활성 PM이 하나도 없을 때만). `projectCode`, `newPmUserId`. 응답은 재지정 후 PM 목록(`pms`) |
| GET | `/api/projects/{projectId}/phases` | USER·EXECUTIVE | 사용자 정의 단계·게이트를 순서대로 조회. 공개 범위는 상세와 같고 단계가 없으면 빈 배열 |
| POST | `/api/projects/{projectId}/phases` | PM | 단계 추가(최대 20개) |
| PATCH | `/api/projects/{projectId}/phases/{phaseId}` | PM | 단계 이름 변경 |
| PUT | `/api/projects/{projectId}/phases/order` | PM | 전체 단계 순서 변경 |
| DELETE | `/api/projects/{projectId}/phases/{phaseId}` | PM | 단계 삭제. WBS에서 참조 중이면 409(phase 연결 도입 후) |
| GET | `/api/projects/{projectId}/wbs/tasks` | USER·EXECUTIVE | WBS 트리(작업 표시 순서, 각 작업 아래 하위작업). 공개 범위는 상세와 같음, WBS 기능을 끄면 404 `FEATURE_DISABLED` |
| POST | `/api/projects/{projectId}/wbs/tasks` | PM · 개발자·QA(담당 작업 하위) | 작업(`parentTaskId` 없음)·하위작업 등록. 최상위 작업의 `phaseId`는 같은 프로젝트 단계여야 하며 생략 시 첫 단계, 하위작업은 상위 단계 상속. 응답은 `taskId`와 바뀐 뒤 트리 |
| PATCH | `/api/projects/{projectId}/wbs/tasks/{taskId}` | PM · 개발자·QA(본인 담당) | 이름·설명·담당자(PM만)·기간·예상 공수·최상위 작업 단계 덮어쓰기. `version` 불일치 409 |
| PUT | `/api/projects/{projectId}/wbs/tasks/{taskId}/position` | PM | 드래그 이동: `parentTaskId`(null=최상위), `index`(0부터), `version` |
| DELETE | `/api/projects/{projectId}/wbs/tasks/{taskId}` | PM | 삭제. 하위작업·선후행·마일스톤 연결이 있으면 409 `WBS_TASK_IN_USE`. 변경 이력은 남고 `DELETED` 이력 추가 |
| GET | `/api/projects/{projectId}/wbs/tasks/{taskId}/predecessors` | USER·EXECUTIVE | 완료→시작 선행 작업 목록과 현재 날짜 기준 지연 일수·후행 영향 여부 |
| PUT | `/api/projects/{projectId}/wbs/tasks/{taskId}/predecessors` | PM · 개발자·QA(본인 담당) | `predecessorTaskIds` 전체 교체. 빈 배열은 모두 해제, 다른 프로젝트·자기 참조 400, 순환 400 |
| GET | `/api/projects/{projectId}/tasks/{taskId}` | USER·EXECUTIVE | 업무 상세(WBS 항목 필드 + 유형·우선순위·완료 조건·완료 시각·칸반 컬럼·상태 변경 시각·등록자). 공개 범위는 WBS와 같음, WBS·칸반 둘 다 꺼야 404 `FEATURE_DISABLED` |
| PATCH | `/api/projects/{projectId}/tasks/{taskId}` | PM · 개발자·QA(본인 담당) | 유형·우선순위·완료 조건 덮어쓰기. `version`(WBS 수정과 공유) 불일치 409 |
| PATCH | `/api/projects/{projectId}/tasks/{taskId}/progress` | PM · 개발자·QA(본인 담당) | 진척률 0~100 수정. 하위작업이 있으면 상위 작업은 직접 수정 불가, 예상 공수 가중 평균으로 자동 갱신 |
| GET | `/api/projects/{projectId}/tasks/{taskId}/histories` | USER·EXECUTIVE | 업무 변경 이력(커서, id 오름차순 = 시간순). `activityType`, `before`·`after`(JSON), `actor`, `occurredAt` |
| GET | `/api/projects/{projectId}/board` | USER·EXECUTIVE | 칸반 보드: 컬럼(표시 순서)과 컬럼별 카드(작업·하위작업, 종료일·ID 순). `assigneeUserId`로 담당자 필터(카드만 줄고 `cardCount`·`wipExceeded`는 전체 기준). 칸반 기능 꺼짐 404 |
| PUT | `/api/projects/{projectId}/board/cards/{taskId}/column` | PM · 개발자·QA(본인 카드) | 카드 이동 `{columnId, version}` → 상태 = 컬럼 상태, DONE이면 완료 시각·진척률 100. 응답 `card`·`column`·`wipWarning`(한도 초과 경고, 이동은 반영) |
| POST | `/api/projects/{projectId}/board/columns` | PM | 컬럼 추가 `{name(비우면 기본 이름), statusCategory, wipLimit}`. 맨 뒤, 프로젝트당 20개 |
| PATCH | `/api/projects/{projectId}/board/columns/{columnId}` | PM | 이름·WIP 한도 덮어쓰기(null = 기본 이름·한도 없음) |
| PUT | `/api/projects/{projectId}/board/columns/order` | PM | 컬럼 순서 `{columnIds}`(모든 컬럼을 한 번씩) |
| DELETE | `/api/projects/{projectId}/board/columns/{columnId}` | PM | 컬럼 삭제. 카드가 있으면 409 `BOARD_COLUMN_NOT_EMPTY`, 상태별 마지막 컬럼이면 409 `BOARD_COLUMN_LAST_OF_STATUS` |
| GET | `/api/projects/{projectId}/wbs/milestones` | USER·EXECUTIVE | 마일스톤 전체(목표일·ID 순, 페이지 없음, 최대 200개). 항목마다 `dDay`·`delayed`·연결/미완료 작업 수·연결 작업 요약 |
| GET | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | USER·EXECUTIVE | 마일스톤 상세(목록 항목과 같은 형식) |
| POST | `/api/projects/{projectId}/wbs/milestones` | PM | 등록: `name`, `targetDate`(프로젝트 기간 안), `taskIds`(선택, 같은 프로젝트 작업, 최대 200). 출처 MANUAL |
| PATCH | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | PM | 이름·목표일 덮어쓰기. `version` 불일치 409 |
| PUT | `/api/projects/{projectId}/wbs/milestones/{milestoneId}/tasks` | PM | 연결 작업 전체 교체(`taskIds`, 빈 배열=모두 해제, `version`). 바뀌면 version +1 |
| DELETE | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | PM | 삭제(연결 정보 함께 삭제, 작업은 남음). 감사 로그 `MILESTONE_DELETED` |
| POST | `/api/projects/{projectId}/wbs/ai-draft` | PM | AI WBS 초안 생성(저장 없음). 응답에 초안과 1회용 `draftToken`(1시간) |
| POST | `/api/projects/{projectId}/wbs/ai-draft/apply` | PM | PM이 승인·수정한 항목과 각 작업의 `phaseId`를 WBS에 반영. 생략 시 첫 단계, 다른 프로젝트 단계면 400. 응답은 tempId → 정식 ID |

### 인증 방식 (세션 + JWT)

1. 로그인 성공 시 Redis에 세션을 만들고, 세션 ID(`sid`)를 담은 JWT 두 개를 HttpOnly 쿠키로 내려준다.
   - `AccessToken` (10분, path `/`), `RefreshToken` (12시간, path `/api/auth`). 둘 다 브라우저 세션 쿠키, SameSite=Lax.
2. 요청마다 AccessToken 검증 → Redis 세션 존재 확인 → 세션 TTL을 30분으로 갱신. 역할·비밀번호 변경 강제 여부는 세션 값으로 판단한다.
3. 30분 동안 API 요청이 없으면 Redis 세션이 사라져 AccessToken·RefreshToken 모두 거부된다(NFR-04). 토큰 재발급(refresh)은 활동으로 치지 않는다.
4. 로그아웃·비밀번호 변경·관리자 초기화는 Redis 세션을 지워 즉시 무효화한다.

| 상황 | 응답 | FE 처리 |
|---|---|---|
| 쿠키 없음 | 401 `로그인이 필요합니다` | 로그인 화면 |
| AccessToken 만료 | 401 `인증 토큰이 만료되었습니다` | `/api/auth/refresh` 후 원 요청 재시도 |
| 세션 만료·로그아웃됨 | 401 `세션이 만료되었습니다…` | 로그인 화면 |
| 비밀번호 변경 필요 | 403 `비밀번호를 변경한 뒤…` | 비밀번호 변경 화면 |
| 권한 없음 (/api/admin/**, 시스템 관리자의 /api/projects/**) | 404 | 존재 비노출 |

### 로그인 규칙

- 없는 ID와 틀린 비밀번호는 같은 400 오류로 응답한다(계정 존재 비노출). 로그에는 구분해 남긴다.
- 5회 연속 실패 시 잠금(423). 성공하면 연속 실패 횟수 초기화. 잠금은 관리자 비밀번호 초기화로만 풀린다.
- 비활성 계정은 비밀번호가 맞을 때 403.
- 비밀번호 규칙: 영문·숫자·특수문자 각 1자 이상, 8~64자, 공백·비ASCII 불가. 대문자 필수 아님.
- CSRF: 토큰 대신 SameSite=Lax 쿠키로 막는다. FE와 BE가 nginx로 같은 origin에서 서비스된다는 전제이며, origin이 달라지면 재검토해야 한다. CORS는 설정하지 않았다(same-origin).
- pagination·idempotency: 이 기능에는 해당 없음(목록 API 없음, 로그인은 재시도해도 새 세션만 생김).

### 사용자·조직 관리 규칙 (FR-ADM-02)

- 사용자는 1개 부서에 소속된다. 시스템 관리자가 아니면 부서 필수, 비활성 부서에는 새로 배치할 수 없다.
- 아이디: 영문·숫자·`.`·`_`·`-`, 50자 이하.
- 프로젝트 생성 권한은 따로 부여하지 않는다(최종안 3.1: 활성 일반 사용자 전원 생성 가능). 기존 `users.can_create_project`와 권한 부여 API는 V5에서 삭제했다.
- 본인 계정의 역할·상태는 바꿀 수 없고, 마지막 활성 시스템 관리자는 강등·비활성화할 수 없다.
- 부서 이름은 같은 상위 부서 안에서 중복 불가(최상위끼리 포함). 자기 자신·하위 부서 밑으로 이동 불가. 활성 소속 인원이나 활성 하위 부서가 있으면 비활성화 불가.
- 사용자·부서는 삭제하지 않고 비활성화한다. 모든 변경과 관리자 비밀번호 초기화는 `audit_logs`에 변경 전·후 값·변경자·IP와 함께 남는다(임시 비밀번호는 기록하지 않음).

### 프로젝트 생성·조회 규칙 (FR-PRJ-01)

- 생성 권한 제한은 없다. 활성 일반 사용자는 누구나 생성하고 생성자가 첫 PM이 된다. 경영진(읽기 전용)·비활성 사용자는 403 `PROJECT_CREATION_FORBIDDEN`, 시스템 관리자는 프로젝트 API 자체가 404. PM은 `project_members.actor = PM`으로만 관리하고, `created_by`는 최초 생성자 기록이다.
- 단계형 화면(① 기본 정보 → ② 템플릿·운영 방식 → ③ 액터 → ④ 기능 구성 → ⑤ 멤버 → ⑥ 게이트 승인자)의 입력은 FE가 모아 마지막에 한 번 `POST /api/projects`로 보낸다. 한 트랜잭션으로 프로젝트·주관 부서·액터·기능·멤버·단계를 만들어, 반쯤 만든 프로젝트가 남지 않는다.
- 필수: 이름(100자 이하), 코드, 공개 범위, 템플릿, 운영 방식, 부서 유형, 액터, 기능 구성(빈 배열 허용). 선택: 기간(둘 다 있으면 시작일 ≤ 종료일, 과거 허용), 주관 부서(0~20개, 모두 활성, 중복 불가). 코드는 영문·숫자로 시작하는 영문·숫자·하이픈 2~20자, 대문자로 저장한다(대소문자만 다르면 중복).
- 템플릿(`EMPTY`·`IT_PROJECT`)은 ②~④의 기본값만 정한다(선택지 API의 `templates`). 사용자가 바꾼 조합도 서버가 받아들이고, 템플릿은 프로젝트에 기록만 한다.
- 기능 구성(FR-PRJ-06): `WBS`·`KANBAN`·`BOARD`·`MEETING`·`DELIVERABLE`(산출물·게이트)·`ISSUE`·`FILE`·`GIT`. `GIT`은 FR-DEV-01 전까지 비활성, `DELIVERABLE`은 단계가 하나 이상일 때만 켤 수 있다(400 `FEATURE_NOT_AVAILABLE`).
- MVP 선택 범위: 운영 방식은 단계 없음(`NONE`)·폭포수, 부서 유형은 SW만, 액터는 PM·현업 담당자(`BUSINESS_OWNER`)·개발자·QA만. 비활성 값은 FE가 막더라도 서버에서 다시 거부한다. 활성 여부는 `Methodology`·`DepartmentType`·`ProjectFeature` enum의 `enabled`가 기준이라, 애자일(FR-PRJ-10)·제조 모듈(4.14)을 열 때 enum 값만 바꾸면 선택지 API와 검증이 함께 바뀐다.
- 액터: PM은 필수, 나머지는 선택. 초대 멤버의 역할은 지정한 액터 중에서만 고르고 PM 역할은 줄 수 없다. 한 사람은 한 프로젝트에서 역할 하나.
- 초대 대상: 활성 일반 사용자(다른 부서 포함). 생성자 본인, 경영진(프로젝트 읽기 전용), 시스템 관리자, 비활성·없는 사용자는 거부.
- 게이트 승인자(선택, 단계가 하나 이상일 때): 초대한 멤버 중 PM이 아닌 1명. 생성되는 모든 단계 게이트에 적용된다. 이후 지정·변경과 미지정 시 단계 전환 차단은 FR-PRJ-05 범위.
- 공개 범위: 전사 공개는 전원, 부서 공개는 주관 부서 중 하나에 소속된 사용자(하위 부서 제외)와 멤버, 멤버 전용은 멤버만 본다. 주관 부서가 없는 부서 공개 프로젝트는 멤버만 본다. 경영진은 주관 부서 중 하나라도 본인 부서·모든 하위 부서에 속한 프로젝트를 멤버 전용까지 보고, 그 밖은 전사 공개만 본다. 상세에서 볼 수 없는 프로젝트는 404로 존재를 드러내지 않는다.
- 상세 응답은 운영 방식과 무관한 공통 정보만 담고, 운영 방식별 구조는 별도 API로 조회한다(폭포수: `/phases`, 애자일은 FR-PRJ-10에서 별도 API 추가). 운영 방식이 늘어도 상세 계약이 바뀌지 않게 하기 위해서다. FE는 `methodology`로 어느 구조 API를 부를지 고른다(목록에도 있어 상세와 병렬 호출 가능).
- 멤버는 인원 제한이 없어 상세에 넣지 않고 커서 목록(`/members`)으로 따로 조회한다. 커서가 id만 담으므로 역할순이 아니라 등록순(id 오름차순)이다. 생성 직후에는 생성자 PM이 첫 행이지만 PM은 여러 명일 수 있어 FE는 `actor`나 상세의 `pms`로 PM을 구분한다.
- 목록 정렬은 최신 생성순 하나만 제공한다. 종료일·이름순 정렬과 리스크 등급 필터(FR-AIR-06)는 후속.
- 프로젝트 상태(`PLANNED`·`IN_PROGRESS`·`ON_HOLD`·`COMPLETED`·`CANCELLED`, 생성 시 `PLANNED`)는 명세에 정의가 없어 팀 확정 전 초안이다. 상태 변경 API는 없다.

### 멤버·역할 관리 규칙 (FR-PRJ-04)

- 권한: 조회(멤버 목록·이력)는 상세를 볼 수 있는 사람 전원, 변경(초대·역할 변경·제외)은 PM만(복수 PM은 동일 권한). 볼 수 없는 프로젝트는 404, 볼 수 있지만 PM이 아니면 403 `PROJECT_PM_ONLY`(경영진 포함).
- 복수 PM: 프로젝트당 PM 1명 이상. PM은 다른 멤버의 역할을 `PM`으로 바꿔 추가하고(`PM_ADDED`), PM을 다른 지정 액터로 바꿔 해제한다(`PM_RELEASED`). 마지막 PM은 해제·제외할 수 없다(409 `LAST_PM_REQUIRED`). 초대로는 PM을 줄 수 없다(초대 후 역할 변경). 비활성 사용자는 PM으로 추가할 수 없다(400 `INVALID_PM_TARGET`). 단일 PM 위임 API(`/pm-delegation`)는 폐지했다.
- 초대: 생성 시 초대와 같은 기준(활성 일반 사용자, 지정 액터 중 PM 외 역할). 이미 멤버·요청 안 중복·초대 불가 사용자가 하나라도 있으면 아무도 추가하지 않는다.
- 게이트 승인자: 역할 변경은 허용, 멤버 제외는 409 `GATE_APPROVER_NOT_REMOVABLE`, PM이 될 수 없다(PM 추가·재지정 모두 400 `PM_TARGET_IS_GATE_APPROVER`). 승인자 변경(FR-PRJ-05) 전까지는 승인자인 멤버를 제외할 수 없다.
- PM 재지정(요구사항 명세서 3.1 예외): 활성 PM이 하나도 남지 않았을 때만 시스템 관리자가 프로젝트 코드와 새 PM 사용자 ID로 배치한다(인사 발령 기준이라 관리자용 프로젝트 조회 API는 두지 않음). 새 PM이 멤버면 역할을 PM으로 바꾸고, 아니면 PM으로 추가한다. 남아 있던 비활성 PM은 모두 멤버에서 뺀다. 없는 코드와 활성 PM이 있는 프로젝트는 같은 409 `PM_REASSIGN_NOT_ALLOWED`로 응답해 프로젝트 존재를 드러내지 않는다.
- 이력: 변경마다 `project_member_histories`(업무 이력, 프로젝트와 함께 보관)와 `audit_logs`(감사 로그, 1년 보관)를 같은 트랜잭션에 남긴다. 한 행은 사람 한 명의 역할 변화이며 재지정은 새 PM 한 행과 제외된 비활성 PM마다 한 행이다. 기존 `PM_DELEGATED` 이력은 조회용으로 남는다. FR-PRJ-01 생성 시 초대한 멤버는 이력 없이 시작한다(`PROJECT_CREATED` 감사 로그에 남음).
- 동시 수정: PM 구성이 바뀔 수 있는 변경(역할 변경·제외·재지정)은 `projects` 행을 비관적 잠금(`SELECT ... FOR UPDATE`)으로 잡고 처리해, 두 PM이 동시에 서로를 해제해도 PM이 0명이 되지 않는다. 잠금 획득 실패는 409 `CONCURRENT_MODIFICATION`. 같은 사용자를 동시에 초대하면 `uk_project_members_project_id_user_id` 위반을 409 `ALREADY_PROJECT_MEMBER`로 바꾼다.

### 계층형 WBS 규칙 (FR-WBS-01)

- 계층: DB는 프로젝트 단계(`phase_id`, nullable) > 작업(`parent_task_id` NULL) > 하위작업 2단계(ERD 04). 하위작업 아래 등록·이동, 하위작업을 가진 작업을 다른 작업 아래로 옮기기는 400 `INVALID_WBS_HIERARCHY`.
- 권한(3.3 WBS-01): 판정 순서 404 `PROJECT_NOT_FOUND`(볼 수 없음) → 404 `FEATURE_DISABLED`(WBS 기능 꺼짐) → 403. PM은 전체, 개발자·QA는 본인이 담당한 작업 아래 하위작업 추가(담당자는 비우거나 본인)·본인 담당 항목 수정만(그 밖 403 `WBS_TASK_ASSIGNEE_ONLY`), 담당자 변경·최상위 작업 등록·이동·삭제는 PM만(403 `PROJECT_PM_ONLY`). 현업 담당자·경영진·멤버 아닌 열람자는 조회만.
- 상위 기간 자동 산출: 하위작업이 있는 작업의 `start_date`·`end_date`는 하위작업의 최소 시작일~최대 종료일로 저장한다. 하위작업 등록·기간 수정·이동·삭제와 AI 초안 반영 때 같은 트랜잭션에서 다시 맞춘다. 간트·지연 감지 등은 저장값을 그대로 읽으면 된다. 이런 작업의 기간을 직접 바꾸면 400 `WBS_SCHEDULE_DERIVED`(응답 `scheduleDerived: true`). 마지막 하위작업이 빠지면 마지막 산출값이 작업 자신의 기간으로 남는다.
- 값 규칙: 시작일 ≤ 종료일, 프로젝트 기간이 있으면 그 안(있는 쪽만, 400 `INVALID_WBS_SCHEDULE`). 하위작업 기간은 상위 작업 기간에 묶지 않는다(상위가 하위로 산출되므로). 담당자는 프로젝트 멤버이면서 활성 계정(400 `INVALID_TASK_ASSIGNEE`), 바꿀 때만 검증.
- 동시성: 모든 변경은 프로젝트 행을 잠그고(`SELECT ... FOR UPDATE`) 시작해 같은 프로젝트의 WBS 변경을 차례로 처리한다(AI 초안 반영 포함). 수정·이동은 `version`이 다르면 409 `CONCURRENT_MODIFICATION`. 같은 값 수정·같은 위치 이동 재전송은 version을 보지 않고 성공(멱등). 형제 순서 재정렬은 `version`을 올리지 않는다(구조 정보는 프로젝트 잠금으로 보호).
- 이동: `index`는 옮길 곳 형제 목록(옮기는 항목 제외)에서 0부터 센 위치, 형제 수보다 크면 400 `INVALID_WBS_POSITION`. 옛·새 부모의 형제 순서를 1부터 다시 매긴다.
- 삭제: PM만. 삭제 정책 미정(ERD 04 3절)이라 하위작업·선후행·마일스톤 연결이 하나라도 있으면 409 `WBS_TASK_IN_USE`(연쇄 삭제 없음). 감사 로그 `WBS_TASK_DELETED`(대상 `WBS_TASK`, 프로젝트·상위 작업·이름). 등록·수정·이동은 감사 로그 대상이 아니다(업무 변경 이력은 FR-TSK-01).
- 트리는 페이지로 나누지 않는다(드래그 편집에 전체가 필요). 대신 프로젝트당 항목(작업+하위작업) 2,000개 상한(409 `WBS_TASK_LIMIT_EXCEEDED`, NFR-02 1,000건의 2배). 조회는 쿼리 2회(항목 전체, 담당자 이름)로 조립한다.
- 범위 밖: 상태 변경(칸반 FR-TSK-02, 아래). 업무 상세 필드·변경 이력은 아래 FR-TSK-01. 새 작업은 칸반 대기(TODO) 첫 컬럼에 놓인다.

### 업무 상세·변경 이력 규칙 (FR-TSK-01)

- 업무 = WBS 작업·하위작업(`wbs_tasks`)이라 ID가 같다. 등록·기본 필드·담당자 배정/해제(PM만)·이동·삭제는 FR-WBS-01 API, 상세 필드(`taskType`·`priority`·`completionCriteria`)는 `/api/projects/{projectId}/tasks/{taskId}`. WBS 등록 요청에도 세 필드를 선택으로 넣을 수 있다(비우면 TASK·MEDIUM·없음). 트리 항목에는 `taskType`·`priority`가 더해졌다(추가 필드, 기존 계약 유지).
- 권한(3.3 TSK-01): 조회·이력 조회는 프로젝트를 볼 수 있는 사용자 전원(현업 담당자·경영진 R 포함). 상세 필드 수정은 PM, 그 업무를 담당한 개발자·QA(그 밖 403 `WBS_TASK_ASSIGNEE_ONLY`). 판정 순서·프로젝트 행 잠금·같은 값 멱등·`version` 409는 WBS와 같다. 다른 프로젝트의 업무 ID는 404 `WBS_TASK_NOT_FOUND`.
- 변경 이력(`task_histories`, append-only): `TaskHistoryRecorder`가 변경과 같은 트랜잭션에서(MANDATORY) 남긴다. `CREATED`(직접 등록·AI 초안 반영), `ASSIGNED`·`UNASSIGNED`(담당자), `DATE_CHANGED`(직접 수정과 하위작업 변경에 따른 상위 기간 산출), `DETAILS_CHANGED`(이름·설명·예상 공수·유형·우선순위·완료 조건 중 바뀐 필드만), `MOVED`(부모·순서, 형제 재정렬은 기록 안 함), `DELETED`(삭제 직전 값), 칸반 카드 이동의 `STATUS_CHANGED`(status·boardColumnId)·`BOARD_COLUMN_CHANGED`(같은 상태 다른 컬럼)·`PROGRESS_CHANGED`(완료 전환 시 100). 전·후 값은 서버가 정한 필드만 JSONB로 저장하고, 화면은 문자열 값을 텍스트로만 표시한다.
- 진척률(FR-TSK-04, SCRUM-119): 말단 작업은 0~100을 직접 입력하고 완료 상태 전환 시 100으로 자동 반영한다. 상위 작업은 하위작업, 단계와 프로젝트는 최상위 작업의 예상 공수 가중 평균(정수 반올림)이다. 예상 공수가 없으면 1인일 가중치로 포함한다. 프로젝트 상세와 단계 목록 응답에 집계 `progress`를 제공한다.
- 삭제와 이력(결정·미결): `task_histories.task_id`에는 FK를 두지 않는다(ERD 05의 FK와 다름). 이력은 지우지 않으므로 FK(NO ACTION)를 두면 이력이 있는 작업을 지울 수 없게 된다. 작업 삭제는 막지 않고 이력(삭제 이력 포함)을 남긴다. 지운 업무의 이력은 API로 조회할 수 없다(업무 404). 삭제 정책이 정해지면 FK 추가 여부를 다시 정한다.
- 담당자 TODO·일정 반영(FR-PSN-01·02): 별도 저장 없이 FR-PSN-01(SCRUM-111)이 조회 시점에 `wbs_tasks.assignee_user_id`로 합친다. 일정은 Sprint 3.
- 완료 시각(`completed_at`): 칸반 카드 이동(FR-TSK-02)이 DONE으로 바꿀 때 `WbsTask.markCompleted`, 되돌릴 때 `clearCompleted`로 기록한다. 이 API는 바꾸지 않는다.
- 기능 모듈: 업무 상세·수정·이력 API는 WBS·칸반 중 하나만 켜져 있어도 쓸 수 있다(칸반 카드에서도 같은 업무를 연다). 등록·담당자·기간은 WBS API라 WBS 모듈이 필요하다.

### 칸반 보드 규칙 (FR-TSK-02)

- 카드 = 업무 = WBS 작업·하위작업 전부(업무 CRUD와 같은 단위). 하위작업 카드에는 상위 작업 이름을 같이 준다. 컬럼 안 카드 순서는 저장하지 않고 종료일·ID 순으로 보여 준다(같은 컬럼 안 순서 변경은 범위 밖).
- 컬럼(`board_columns`, V12): 프로젝트를 만들면 운영 방식·템플릿·칸반 모듈 사용 여부와 관계없이 기본 4개(대기·진행·검토·완료 = `TODO`·`IN_PROGRESS`·`REVIEW`·`DONE`)를 같은 트랜잭션에서 만든다(`ProjectDefaultDataInitializer` → `DefaultBoardColumnsInitializer`). 컬럼마다 매핑 상태(`statusCategory`, TASK_STATUS 코드)가 있고 만든 뒤 바꾸지 않는다.
- 컬럼 이름과 다국어: 기본 컬럼은 이름을 저장하지 않고(`name` NULL) 응답 `labelKey`(`code.TASK_STATUS.<상태>`)의 다국어 문구로 보여 준다. 사용자가 붙인 이름은 사용자 입력이라 라벨 키가 아니며 입력한 그대로 보여 준다(50자, 줄바꿈·탭 등 제어 문자 불가, 비우면 기본 이름으로 되돌림).
- 카드 이동: 상태는 옮긴 컬럼의 상태가 된다. 상태가 바뀔 때만 `status_changed_at`(FR-AIR-02 상태 체류, NULL = 등록 이후 변경 없음 → 기준 `created_at`)을 남기고, DONE이 되면 `completed_at`과 진척률 100(FR-TSK-04 '완료 상태 전환 시 100% 자동 반영'), DONE에서 벗어나면 `completed_at`만 지운다(진척률 유지, 진척 입력은 FR-TSK-04). 같은 상태의 다른 컬럼으로 옮기면 컬럼만 바뀐다. 이력은 같은 트랜잭션.
- 권한(3.3 TSK-02): 조회는 프로젝트를 볼 수 있는 사용자 전원. 카드 이동은 PM(전체)·개발자·QA(본인 담당 카드, 그 밖 403 `WBS_TASK_ASSIGNEE_ONLY`). 현업 담당자(담당이어도)·경영진·비멤버는 조회만. 컬럼 관리는 PM만(403 `PROJECT_PM_ONLY`). 판정 순서: 404 `PROJECT_NOT_FOUND` → 404 `FEATURE_DISABLED`(KANBAN) → 404 `WBS_TASK_NOT_FOUND` → 403 → 404 `BOARD_COLUMN_NOT_FOUND` → 같은 컬럼 200(멱등) → 409 `CONCURRENT_MODIFICATION`(업무 `version`, WBS·업무 API와 공유).
- WIP: `wipLimit`(1~999, NULL = 없음)보다 카드 수가 많으면 `wipExceeded`·이동 응답 `wipWarning`이 true. 경고만 하고 이동은 막지 않는다(인수 기준). 카드 수는 필터와 무관한 컬럼 전체 기준.
- 컬럼 불변식: 상태마다 컬럼 1개 이상(마지막이면 삭제 409 `BOARD_COLUMN_LAST_OF_STATUS`), 카드가 있는 컬럼 삭제 409 `BOARD_COLUMN_NOT_EMPTY`(카드 연쇄 이동 없음), 프로젝트당 20개(409 `BOARD_COLUMN_LIMIT_EXCEEDED`), 순서 변경은 모든 컬럼 ID를 한 번씩(400 `INVALID_BOARD_COLUMN_ORDER`). 새 작업(WBS 등록·AI 초안 반영)은 대기 상태 첫 컬럼에 놓인다. 컬럼이 비어 있는 작업은 같은 상태의 첫 컬럼에 보인다.
- 동시성: 카드 이동·컬럼 관리도 WBS 변경과 같은 프로젝트 행 잠금으로 직렬화한다(컬럼 삭제와 카드 이동·작업 등록 경쟁 방지). `board_columns`에는 version이 없어(ERD 05) 같은 컬럼을 두 PM이 고치면 나중 요청이 이긴다.
- 감사 로그(설정 변경·삭제): `BOARD_COLUMN_CREATED`·`BOARD_COLUMN_CHANGED`·`BOARD_COLUMN_DELETED`(대상 `BOARD_COLUMN`), `BOARD_COLUMNS_REORDERED`(대상 `PROJECT`). 카드 이동은 업무 이력으로 남긴다.
- 조회 비용: 컬럼 1회 + 업무 1회 + 담당자 이름 1회(N+1 없음). 업무는 프로젝트당 최대 2,000개라 WBS 트리처럼 페이지 없이 한 번에 준다. 카드 이동의 WIP 판정과 컬럼 삭제의 카드 유무는 보드 전체를 읽지 않고 COUNT 쿼리로 센다(그 컬럼에 놓인 카드 + 그 상태의 첫 컬럼이면 컬럼이 비어 있는 같은 상태 카드, 조회 배치 규칙과 같음).
- 보드 조회 필터: `assigneeUserId`와 `phaseId`를 각각 또는 함께 지정한다. `phaseId`는 같은 프로젝트 단계여야 하며 카드 응답에도 포함된다. 같은 컬럼 안 카드 순서와 상위 작업 상태의 하위작업 연동은 후속 범위다.

### 선후행 관계 규칙 (FR-WBS-03)

- 현재 지원 유형은 ERD의 `FINISH_TO_START`(완료→시작)뿐이다. 후행 작업별 선행 작업 ID 전체를 교체하며 중복은 한 번만 저장하고, 빈 배열은 모두 해제한다. 같은 목록 재전송은 저장하지 않는 멱등 요청이다.
- 권한(3.3 WBS-03): 조회는 프로젝트를 볼 수 있는 사용자 전원. 변경은 PM 전체, 개발자·QA는 본인 담당 후행 작업만 가능하다. 판정 순서는 404 `PROJECT_NOT_FOUND` → 404 `FEATURE_DISABLED` → 404 `WBS_TASK_NOT_FOUND` → 403이다.
- 선행 작업은 같은 프로젝트의 다른 WBS 작업만 허용한다. 자기 참조·다른 프로젝트·없는 작업은 존재 여부를 구분하지 않고 400 `INVALID_WBS_DEPENDENCY`, 직접·간접 순환은 400 `WBS_DEPENDENCY_CYCLE`이다.
- 변경은 다른 WBS 편집과 같은 프로젝트 행 잠금으로 직렬화한다. 전체 프로젝트 그래프를 위상 정렬로 검증한 뒤 빠진 연결만 삭제하고 새 연결만 추가한다. ERD의 유일 제약과 인덱스를 그대로 사용하며 스키마 변경은 없다.
- 지연은 조회 시 한국 날짜 기준으로 계산한다. 선행 작업이 미완료이고 종료일이 오늘보다 전이면 `delayed=true`, `delayDays=종료일~오늘`이며 후행 작업도 미완료이면 `impactsSuccessor=true`이다. 저장값은 변경하지 않는다.

### 마일스톤 규칙 (FR-WBS-04)

- 권한(3.3 WBS-04): 조회는 프로젝트를 볼 수 있는 사용자 전원(멤버, 공개 범위 열람자, 소속 범위 경영진). 등록·수정·연결·삭제는 PM만(403 `PROJECT_PM_ONLY`). 판정 순서와 프로젝트 행 잠금은 계층형 WBS와 같다. 다른 프로젝트의 마일스톤 ID는 404 `MILESTONE_NOT_FOUND`.
- '오늘'(업무 날짜): 주입한 `Clock`의 시점을 `BusinessCalendar.ZONE`(Asia/Seoul) 날짜로 바꾼다. 서버·JVM 기본 시간대는 쓰지 않는다. 근거: 프로젝트·작업 기간과 목표일은 회사 달력 날짜(DATE)이고 본사·MVP 사용 조직이 한국에 있다. 거점 시간대별 마감 표시는 NFR-19에서 후속(C).
- D-day: `dDay = 목표일 - 오늘`(일). 양수 D-n, 0 D-day, 음수 D+n(지남). 화면 표기는 FE가 한다.
- 지연 경고(`delayed`): 오늘 ≥ 목표일(당일 포함, 인수 기준의 '기한 도래')이고 연결 작업 중 상태가 `DONE`이 아닌 것이 하나 이상. 연결 작업이 없으면 경고하지 않는다(`linkedTaskCount` 0). 미완료 수(`incompleteTaskCount`)는 경고 전에도 돌려준다. 조회 때마다 규칙으로 계산하고 저장하지 않는다. 연결 작업이 상위 작업이면 그 작업 자신의 상태만 본다.
- 목표일: 프로젝트 기간이 있으면 그 안(있는 쪽만, 400 `INVALID_MILESTONE_TARGET_DATE`). 지난 날짜도 등록 가능.
- 연결 작업: 같은 프로젝트의 작업·하위작업만(없는 ID·다른 프로젝트 ID 모두 400 `INVALID_MILESTONE_TASK`, 존재 여부를 구분하지 않음). 중복 ID는 한 번만 연결. 교체는 빠진 연결 삭제 + 새 연결 추가(남는 연결 행은 유지). 같은 목록 재전송은 version을 보지 않고 200(멱등). 연결이 바뀌면 마일스톤 `version`을 올려 이름 수정과 같은 낙관적 잠금으로 보호한다.
- 삭제: PM만. 연결 정보(`milestone_tasks`)는 마일스톤에 딸린 데이터라 함께 지우고 작업은 남는다(작업 삭제는 마일스톤 연결이 있으면 409로 막는 것과 짝). 감사 로그 `MILESTONE_DELETED`(대상 `MILESTONE`, 프로젝트·이름·목표일·해제한 연결 수).
- 목록은 페이지로 나누지 않는다(간트·대시보드가 전체를 그림). 대신 프로젝트당 200개 상한(409 `MILESTONE_LIMIT_EXCEEDED`, AI 초안 반영에도 적용), 마일스톤당 연결 작업 200개. 연결 작업은 조인 쿼리 한 번으로 읽는다(N+1 없음).
- 범위 밖: 마감 알림(FR-NTF-01, Sprint 3), 마일스톤 지연 예상(FR-AIR-01).

### AI WBS 초안 규칙 (FR-PRJ-07)

- 흐름: FE → `coreviaai_be`(권한·토큰·반영) → `aiserver` 계약 `WbsDraftGenerator`(같은 프로세스 호출). LLM 호출은 aiserver만 한다. FastAPI `/generate/wbs`의 Java 이식 전까지 aiserver는 503 `AI_NOT_READY`를 돌려준다. 민감정보 마스킹은 팀 결정으로 미구현이라 요구사항 원문이 aiserver로 그대로 간다.
- 대상: PM(복수 PM 누구나), 단계가 하나 이상인 프로젝트(0개면 404 `PHASES_NOT_AVAILABLE`), WBS 기능을 켠 프로젝트(끄면 404 `FEATURE_DISABLED`). 방법론과 단계 구성은 독립적이며 프로젝트 기간은 없어도 된다(aiserver에는 null로 보내고, 반영 검증은 있는 쪽만 적용).
- 초안 기록과 승인(NFR-07·08): 생성 결과는 `ai_drafts`에 `DRAFT`(feature `WBS_DRAFT`, AI 결과 JSON·응답 시간)로, AI 실패는 `FAILED`로 남긴다. WBS 테이블은 바꾸지 않는다. 반영하면 같은 트랜잭션에서 `ACCEPTED`(제외 항목이 있으면 `PARTIALLY_ACCEPTED`)와 결정자·시각을 남긴다. 반영하지 않은 초안은 `DRAFT`로 남는다(거절 API 없음). 요구사항 원문 등 사용자 입력은 저장하지 않는다.
- 반영 토큰: Redis에 `draftToken`(프로젝트·PM·`ai_drafts.id`에 묶임, 1시간)을 둔다. 반영 API는 FE가 보낸 승인 항목만 WBS에 저장한다.
- 반영: 토큰을 원자적으로 소비한 뒤 한 트랜잭션에서 PM·기능을 다시 확인하고, 참조·기간·순환·담당자를 서버에서 전부 검증한 뒤 작업·하위작업·선후행·마일스톤·감사 로그를 저장한다. 실패하면 토큰을 되돌려 같은 토큰으로 다시 반영할 수 있다. 이미 반영한 토큰은 409 `AI_DRAFT_ALREADY_APPLIED`.
- 단계 연결: `wbs_tasks.phase_id`로 사용자 정의 단계에 연결한다. AI 초안 생성에는 기존 작업의 `phaseId`를 보내고, 반영 요청의 `phaseId`를 최상위 작업과 하위작업에 함께 저장한다.
- 감사 로그: `AI_WBS_DRAFT_REQUESTED`(`aiDraftId`, 입력 길이·결과·제안 개수. 원문 미기록, AI 호출 뒤 별도 트랜잭션), `AI_WBS_DRAFT_APPLIED`(`aiDraftId`, 반영·제외 개수, 반영과 같은 트랜잭션).
- 반영도 직접 편집과 같은 프로젝트 행 잠금·항목 수 상한(2,000개, 409 `WBS_TASK_LIMIT_EXCEEDED`)을 쓰고, 하위작업이 있는 작업의 기간은 하위작업 기간으로 맞춘다(FR-WBS-01).
- AI 오류는 사용자에게 고정 메시지만 준다: 502 `AI_GENERATION_FAILED`(생성 실패·형식 오류·프로젝트에 없는 단계), 503 `AI_NOT_READY`(이식 전), 504 `AI_TIMEOUT`(이식 후 LLM 시간 초과에 사용).

### 개인 일정 API (FR-PSN-02, SCRUM-120)

- `GET /api/me/events?from&to&page&size`: 본인 일정 중 `[from, to)`와 겹치는 일정. `from < to`, 최대 366일.
- offset 페이지: `page`는 0부터(기본 0), `size`는 1~100(기본 20). `response`는 `{items, page, size, hasNext}`이며 `startAt`, `id` 오름차순이다. `hasNext`일 때 다음 페이지를 요청한다.
- `POST /api/me/events`는 등록(201), `PATCH /api/me/events/{eventId}`는 전체 필드 교체이다. 타인 일정 수정과 없는 일정은 모두 404이다.
- V17은 `personal_events`를 추가한다. develop의 기존 V15·V16을 보존한다. 이미 이전 번호로 적용한 DB에는 자동 repair를 실행하지 말고 이력을 점검한다.
- `GET /api/me/calendar?from&to&page&size`는 개인 일정과 현재 담당 업무를 통합 조회한다. 업무 기간은 한국 날짜의 시작부터 종료일 다음 날 00:00까지이며 재배정·기간·상태 변경을 WBS 원본에서 즉시 읽는다.
- 마일스톤·회의 일정 통합과 프로젝트별 색상은 후속 범위다.
- API 명세: [06-2. 개인 일정·업무일지](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/23396356), [PR #32 인수 조건 평가](docs/reviews/SCRUM-120-pr32.md).

### 업무일지 API (FR-PSN-03, SCRUM-121)

- `GET /api/me/worklogs?from&to`: 날짜 양끝 포함 최대 366일의 본인 노트, 저장된 날짜만 반환한다. 날짜당 1건이므로 최대 366건이다.
- `GET /api/me/worklogs/{logDate}`: 본인 노트 조회, 없으면 404. `PUT`은 생성·교체이며 본문은 최대 10,000자, 같은 내용 재저장 시 수정 시각을 유지한다.
- 첫 동시 저장은 `users` 행 잠금과 `(user_id, log_date)` 유일 제약으로 직렬화한다. 직접 JPA 잠금의 timeout/실패는 원문·DB 정보를 노출하지 않는 409 `CONCURRENT_MODIFICATION`으로 변환한다.
- `PUT /api/me/worklogs/{logDate}/tasks/{taskId}`는 본인 담당 업무의 연결 본문·진척률·당일 소요 시간·완료를 한 트랜잭션으로 반영한다. 완료는 100%를 요구하고 하위업무 변경 시 상위 진척률을 가중 집계한다.
- `GET /api/me/worklogs/{logDate}/tasks`는 본인 연결 기록, `GET /api/projects/{projectId}/worklogs?from&to`는 PM에게 해당 프로젝트의 연결 기록만 반환한다. 개인 날짜별 본문은 PM에게 노출하지 않는다.
- V18은 `worklogs`, V19는 `worklog_tasks`, V20은 사용자 표시 언어를 추가한다.
- [연동 결정](docs/adr/SCRUM-109-121-personal-task-integration.md), [개인 본문 저장 시퀀스](docs/sequence/06-4-worklog-upsert.md), [연결 업무 저장 시퀀스](docs/sequence/06-5-worklog-task-save.md).

### 목록 페이지네이션 (커서)

- 요청: `?cursor={이전 응답의 nextCursor}&size=20` (기본 20, 최대 100). 첫 페이지는 `cursor` 없이 요청한다.
- 응답: `{"items": [...], "nextCursor": "..." | null, "hasNext": true | false}`
- 정렬은 API마다 고정한다: 사용자 목록·변경 이력·프로젝트 목록은 id 내림차순(최신순), 부서 목록은 id 오름차순(상위 부서가 먼저 와서 트리 구성이 쉬움), 프로젝트 멤버 목록은 id 오름차순(등록순), 프로젝트 멤버 이력은 id 내림차순(최신순).
- 커서는 불투명 문자열이며 형식이 틀리면 400 `INVALID_CURSOR`. size+1개를 읽어 다음 페이지 여부를 판단한다(count 쿼리 없음).

### 중복 요청(idempotency)

- 사용자 추가: `loginId`가 자연 키다. 같은 요청을 다시 보내면 409 `DUPLICATE_LOGIN_ID`로 끝나고 계정은 하나만 생긴다. 별도 Idempotency-Key는 쓰지 않는다.
- 부서 추가: (상위 부서, 이름)이 자연 키다. 재전송은 409 `DUPLICATE_DEPARTMENT_NAME`.
- AI 초안 반영: `draftToken`이 1회용 키다. 재전송·동시 반영은 409 `AI_DRAFT_ALREADY_APPLIED`. 초안 생성은 부수효과가 없어 재전송하면 새 초안·새 토큰이 나온다.
- 프로젝트 생성: `code`가 자연 키다. 재전송은 409 `DUPLICATE_PROJECT_CODE`로 끝나고 프로젝트는 하나만 생긴다(사전 검사 + `uk_projects_code`).
- 멤버 초대: (프로젝트, 사용자)가 자연 키다. 재전송은 409 `ALREADY_PROJECT_MEMBER`. 제외 재전송은 404 `PROJECT_MEMBER_NOT_FOUND`, 역할 변경 재전송은 같은 역할이라 변경 없음.
- 수정·상태 변경(PATCH): 현재와 같은 값이면 변경·이력 없이 현재 상태를 돌려준다. 역할 변경·비활성화는 DB 커밋 후 세션을 끊는데, 세션 저장소 장애로 실패하면 오류가 반환되고 같은 요청을 다시 보내면 세션 끊기만 다시 수행된다.

## 7. Database와 Migration

- 저장소: PostgreSQL (`users`, `local_credentials`, `login_history`, `departments`, `audit_logs`, `projects`, `project_departments`, `project_actors`, `project_features`, `project_members`, `project_phases`, `project_member_histories`, `wbs_tasks`, `wbs_dependencies`, `milestones`, `milestone_tasks`, `task_histories`, `board_columns`, `ai_drafts`, `code_groups`, `codes`, `i18n_labels`, `error_logs`), Redis (세션, AI 초안 토큰). 테이블 정의는 Confluence [BE_ERD](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/folder/12878096) 도메인 페이지 참고. ERD 원본은 `docs/erd/`(draw.io: `01-user-auth`, `02-admin`, `04-wbs`, `05-task-board`), 시퀀스 원본은 `docs/sequence/`.
- 시각은 모두 UTC instant(`timestamptz`)로 저장한다. KST 표시는 응답·화면 경계에서 변환한다.
- 동시성: 로그인 시 `users` → `local_credentials` 순서로 `SELECT … FOR UPDATE`해 같은 계정의 동시 로그인과 실패 횟수 갱신을 순서대로 처리한다. 비밀번호 변경·초기화는 `local_credentials`만 잠근다. 두 테이블 모두 `version`으로 낙관적 잠금도 건다.
- 동시성(관리자 기능): `users`·`departments`는 `version` 낙관적 잠금으로 동시 수정을 막고 충돌 시 409 `CONCURRENT_MODIFICATION`. 마지막 관리자 보호는 활성 관리자 행을 `FOR UPDATE`로 잠근 뒤 확인한다. 아이디·부서 이름 중복은 사전 검사 + DB 유니크 제약으로 막는다(최상위 부서 이름은 NULL 특성상 사전 검사만).
- V2(`V2__create_departments_and_audit_logs.sql`): `departments`, `audit_logs` 생성, `users`에 NULL 허용·기본값 컬럼만 추가해 기존 행과 V1 기준 코드에 영향 없음(expand). 되돌릴 때는 추가 컬럼·테이블을 지우는 새 버전을 추가한다.
- V3(`V3__create_projects.sql`): `projects`, `project_actors`, `project_members`, `project_phases` 테이블만 새로 만든다(기존 테이블 변경 없음). 프로젝트 기간은 달력 날짜라 `DATE`로 저장한다(UTC 시각 아님). enum 컬럼은 `audit_logs.action`과 같이 CHECK 제약 없이 이름을 저장한다. 되돌릴 때는 네 테이블을 지우는 새 버전을 추가한다.
- V4(`V4__create_project_member_histories.sql`): `project_member_histories`만 새로 만든다(기존 테이블 변경 없음). `change_type`은 닫힌 값이라 CHECK 제약을 두고, `before_actor`·`after_actor`는 `project_members.actor`와 같이 CHECK 없이 저장한다. 되돌릴 때는 이 테이블을 지우는 새 버전을 추가한다.
- V5(`V5__align_projects_with_final_spec.sql`, 요구사항명세 최종안 반영): `users.can_create_project` 삭제, `projects.start_date`·`end_date` NULL 허용, `projects.template` 추가(기존 행은 `IT_PROJECT`), `project_departments` 신설 후 `projects.department_id` 이관·삭제, `projects.pm_user_id` 삭제(PM은 `project_members.actor = PM`), `project_features` 신설(기존 행은 IT 템플릿 기본 모듈), 멤버 이력 CHECK에 `PM_ADDED`·`PM_RELEASED` 추가. 컬럼을 지우므로 같은 버전의 애플리케이션과 함께 배포해야 한다(이전 버전은 지운 컬럼을 읽음). 되돌릴 때는 컬럼을 다시 추가하고 새 테이블에서 값을 채우는 새 버전을 추가한다(roll-forward). 실행 전 운영 DB 백업을 확인한다.
- V6(`V6__create_wbs.sql`): `wbs_tasks`, `wbs_dependencies`, `milestones`, `milestone_tasks`만 새로 만든다(기존 테이블 변경 없음). 단계(`project_phases`) 연결 컬럼은 두지 않는다(ERD 04). `source`·`dependency_type`은 닫힌 값이라 CHECK, `wbs_tasks.status`는 공통 코드 TASK_STATUS로 값이 늘 수 있어 CHECK 없음. 업무 상세 컬럼(유형·우선순위·칸반 컬럼 등)은 FR-TSK-01·02에서 추가한다. 되돌릴 때는 네 테이블을 지우는 새 버전을 추가한다.
- V7(`V7__create_ai_drafts.sql`): `ai_drafts`(ERD 10)만 새로 만든다. `feature`·`status`는 닫힌 값이라 CHECK(AI 기능이 늘면 CHECK도 새 버전으로 수정). `output`은 TEXT(AI 결과 JSON). 보관 기간(1년, NFR-07) 정리는 FR-ADM-04. 되돌릴 때는 테이블을 지우는 새 버전을 추가한다.
- V8(`V8__create_code_groups_and_codes.sql`): `code_groups`, `codes`만 새로 만들고 4개 그룹(RISK_LEVEL·TASK_STATUS·REQUEST_RESULT·PROJECT_STATUS)과 코드를 시드한다(기존 테이블 변경 없음). 되돌릴 때는 두 테이블을 지우는 새 버전을 추가한다.
- 공통 코드 규칙(FR-ADM-07, NFR-20): 화면은 상태값을 하드코딩하지 않고 `GET /api/codes/{groupCode}`의 코드·`labelKey`로 표시한다. 코드 값·라벨 키는 고정이고 관리자는 정렬 순서만 바꾼다(재배포 없음). 코드 추가·삭제와 사용 여부(`active`) 변경은 마이그레이션으로만 한다(FR-ADM-07 인수 기준: 코드 값 수정·삭제 불가). 표시 텍스트는 라벨 키로 다국어 라벨(FR-ADM-06)에서 바꾼다. 라벨 키 규칙: 그룹 `codeGroup.<GROUP>`, 코드 `code.<GROUP>.<CODE>`(서버가 만듦, `CommonCode.groupLabelKey`·`codeLabelKey`). 코드 그룹·코드 추가와 enum과 짝인 그룹(TASK_STATUS = `WbsTaskStatus`, PROJECT_STATUS = `ProjectStatus`)의 값 변경은 기능 수정이라 새 마이그레이션 + enum 변경을 함께 한다(`CodeSeedConsistencyTest`가 어긋나면 실패).
- V9(`V9__create_i18n_labels.sql`): `i18n_labels`만 새로 만들고 V8 공통 코드의 라벨 키 18개(그룹 4 + 코드 14)를 한국어·영어·번체 중국어로 시드한다(기존 테이블 변경 없음). 되돌릴 때는 테이블을 지우는 새 버전을 추가한다.
- V10(`V10__create_error_logs.sql`): `error_logs`와 인덱스 `idx_error_logs_occurred_at`, `idx_error_logs_error_type_occurred_at`만 새로 만든다(기존 테이블 변경 없음). 되돌릴 때는 테이블을 지우는 새 버전을 추가한다.
- V11(`V11__add_task_details_and_histories.sql`): `wbs_tasks`에 `task_type`(NOT NULL 기본 'TASK', CHECK TASK·DEFECT)·`priority`(NOT NULL 기본 'MEDIUM', CHECK HIGH·MEDIUM·LOW)·`completion_criteria`·`completed_at`을 추가(expand, 기존 행은 기본값)하고 `task_histories`(JSONB 전·후 값, `actor_user_id` FK, `task_id` FK 없음, `idx_task_histories_task_id_id`)를 만든다. `activity_type`은 코드 enum 기준이라 CHECK 없음. 기존 작업의 과거 이력은 만들지 않는다. 되돌릴 때는 이력 테이블을 지우고 네 컬럼을 삭제하는 새 버전을 추가한다(코드 배포 뒤 contract).
- V12(`V12__create_board_columns.sql`): `board_columns`(CHECK 상태·이름 공백 불가·순서 > 0·WIP > 0, `idx_board_columns_project_id`)를 만들고 `wbs_tasks`에 `board_column_id`(FK, NULL 허용, `idx_wbs_tasks_board_column_id`)·`status_changed_at`을 추가한다. 백필: 모든 기존 프로젝트에 기본 컬럼 4개, 기존 작업은 자기 상태의 기본 컬럼(이전에는 모두 TODO), `status_changed_at`은 NULL. 새 상태 코드를 추가하면 CHECK·`WbsTaskStatus`·V8 시드를 함께 바꾼다. 되돌릴 때는 FK·컬럼·테이블을 지우는 새 버전(코드 배포 뒤 contract).
- 다국어 라벨 규칙(FR-ADM-06, NFR-19·20): 화면 코드는 문구를 하드코딩하지 않고 라벨 키만 쓴다. 문구는 `GET /api/i18n/labels`로 언어별로 받아 표시하고(번역 누락은 한국어), 관리자가 `/api/admin/i18n/labels`에서 바꾸면 다음 조회부터 반영된다(서버 캐시 없음, 재배포 불필요). 라벨 키 형식은 영문자로 시작하고 점으로 구분한 영문·숫자·밑줄·하이픈 100자 이하(예: `menu.project.list`), 문구는 500자 이하·제어 문자(줄바꿈 등) 불가. 빈 번역은 NULL(누락)로 저장한다. 새 화면 문구의 라벨은 관리자 API로 등록하거나 마이그레이션으로 시드한다. 공통 코드는 마이그레이션으로만 추가하므로(FR-ADM-07) 코드 그룹·코드를 추가할 때는 라벨 시드도 함께 넣는다(`LabelSeedConsistencyTest`가 1:1을 검증). 라벨은 화면에 텍스트로만 출력한다(HTML로 해석하지 않음).
- Migration: `src/main/resources/db/migration/V{n}__설명.sql`, 기동 시 Flyway 자동 적용. 이미 적용된 파일은 수정하지 않고 새 버전을 추가한다(roll-forward).
- backup·보존: DB 백업은 NFR-13(일 1회) 기준으로 회사 DB 측 운영. `login_history`·`audit_logs` 1년 보관(NFR-07) 정리 작업은 FR-ADM-04에서 구현 예정. `audit_logs`는 추가만 하고 수정·삭제 API가 없다.

## 8. 외부 연동

| 대상 | timeout | retry | 실패 시 |
|---|---|---|---|
| Redis (세션) | connect 2s, command 2s | 없음 (인증 판정을 추측으로 대신하지 않음) | 503 `외부 서비스에 일시적으로 연결할 수 없습니다` |
| PostgreSQL | HikariCP 기본값 | 없음 | 500 |
| Redis (AI 초안 토큰) | 세션과 같음 | 없음 | 503 |

- timeout 값 근거: 세션 조회는 모든 요청 경로에 있어 NFR-01(2초 응답)을 넘기지 않도록 2초로 정했다. overall deadline은 아직 따로 두지 않았다(미정).
- test 대체: `InMemoryAuthSessionStore`·`InMemoryWbsDraftTokenStore`(src/test/integration), AI 호출은 통합 테스트에서 `WbsDraftAiClient`를 `@MockitoBean`으로 대체, H2. LLM timeout·retry는 aiserver 이식 때 정한다.

## 9. 오류 처리와 관측성

- 오류 응답은 `ErrorCode` + `ApiResponse.error`. stack trace·내부 정보는 응답에 넣지 않는다.
- 에러 로그(FR-ADM-09): `GlobalExceptionHandler`의 최종 `Exception` 처리기가 미처리 예외를 공통 500(`INTERNAL_SERVER_ERROR`)으로 응답하고 `ErrorLogRecorder`로 `error_logs`에 남긴다. 기록은 별도 트랜잭션(REQUIRES_NEW)이라 원래 작업이 롤백돼도 남고, 기록이 실패해도 응답은 같다(실패는 애플리케이션 로그로만). 업무 예외(`CustomException`)·입력 오류·인증/권한 거부·매핑 없는 경로(공통 404 본문)·스프링 표준 4xx(처리기가 다시 던져 기본 처리)는 기록하지 않는다. 요청 본문·헤더(Authorization, Cookie)·쿼리 문자열은 저장하지 않고 사용자는 ID만 남긴다.
- 마스킹(NFR-06 일부): 에러 로그의 메시지·스택(원인 `Caused by` 메시지 포함)·경로는 `SensitiveValueMasker`로 가린 뒤 자른다(메시지 2000자, 스택 50,000자, 원인 8단계·원인별 프레임 30개). 대상: 비밀 키 이름(password·secret·token·authorization·cookie·apiKey·credential·sessionId 등)의 값, JWT, Bearer/Basic 값, PostgreSQL `Key (..)=(값)`·`Failing row contains (..)`, 이메일, 주민등록번호·휴대폰·전화번호 형태. 고객사명·공정 정보는 대상이 아니다. 애플리케이션 로그(`log.error`)는 아직 마스킹하지 않는다(미정).
- 미구현: 에러 로그 1년 보관 정리(NFR-07). 삭제 정책·스케줄러는 감사 로그 정리(FR-ADM-04)와 함께 정한다.
- 비밀번호·토큰은 로그에 남기지 않는다(요청 DTO `toString` 마스킹, 임시 비밀번호 비로깅).
- 미정: correlation ID, metric·trace, SLO·alert, health check(actuator 미도입), 로그 보존 기간·접근 권한, incident runbook. 운영 배포 전 결정 필요.

## 9-1. 문서와 보안 운영 메타데이터

- 문서 담당자·검증일: 상단 참고 (담당자 미정).
- 민감 정보: 비밀번호(해시만 저장), JWT 서명 키, 로그인 IP·감사 로그 IP(개인정보). 감사 로그 details에는 비밀값을 넣지 않는다. 에러 로그는 저장 전 패턴 마스킹하고 시스템 관리자만 조회한다. 분류 기준·로그 접근 권한: 미정.
- secret rotation·incident 대응 절차: 미정.

## 10. 테스트

테스트는 `src/test` 아래 두 폴더로 나눈다(`build.gradle`의 `test`·`integrationTest` source set).

| 폴더 | 종류 | 실행 | 기준 |
|---|---|---|---|
| `src/test/unit/java` | 단위 테스트 | `./gradlew test` | Spring 컨텍스트·DB 없이 도는 domain 규칙·policy·util. 빠르고 결정적 |
| `src/test/integration/java` | 통합 테스트 | `./gradlew integrationTest` | `@SpringBootTest` + MockMvc + H2(PostgreSQL 모드) + 메모리 세션 저장소로 API·DB·보안 경계 검증 |

`./gradlew check`가 둘 다 돌리고 커버리지 리포트(`build/reports/jacoco/test`)를 합쳐 만든다. 통합 테스트는 단위 테스트 보조 클래스(`support.MutableClock`)를 그대로 쓴다.

- unit: `PasswordPolicyTest`, `LocalCredentialTest`(잠금·초기화), `JwtTokenProviderTest`(만료·위조·용도), `TemporaryPasswordGeneratorTest`, `InitialAdminInitializerTest`, `UserTest`, `CursorCodecTest`, `CursorPageTest`, `AuditChangesTest`
- unit(FR-PRJ-01·06·09): `ProjectCreationPolicyTest`(기간 후입력·순서, 주관 부서 0~N·중복, 운영 방식 활성, 템플릿과 다른 조합 허용, 액터, 기능 모듈, 멤버, 게이트 승인자), `ProjectTypeRulesTest`(운영 방식·기능 모듈·템플릿 기본값 일관성)
- unit(FR-PRJ-04): `ProjectMemberPolicyTest`(초대, 역할 변경·PM 추가·해제·최소 1명, 제외)
- integration(FR-ADM-01): `AuthApiIntegrationTest` (시간 이동 가능한 Clock). 미인증 차단, 로그인 성공·실패 기록, 5회 잠금, 비활성 계정, 30분 무활동 만료·연장, 로그아웃, 비밀번호 변경, 관리자 초기화 흐름, 로컬 로그인 정보가 없는 사용자, 모듈별 보안 체인.
- integration(FR-ADM-02): `AdminUserApiIntegrationTest`(추가→비밀번호 발급→로그인·초기화 감사 로그, 중복 재전송, 부서 필수·비활성 부서, 비활성화 로그인 차단·세션 종료, 역할 변경·세션 종료, 본인 변경 금지, 마지막 관리자 보호, 생성 권한 API 폐지, 이력, 커서 목록·필터·잘못된 커서), `AdminDepartmentApiIntegrationTest`, `AdminSessionTerminationFailureTest`
- integration(FR-PRJ-01): `ProjectCreateApiIntegrationTest`(생성자 PM·5단계·승인자·기능·감사 로그, 기간·주관 부서 생략, 복수 주관 부서, 단계 없음 생성, 단계 없음 규칙·Git 비활성, 코드 정규화·중복, 필수 항목, 생성 권한 개방·경영진 403·관리자 404, 비활성 부서, 선택지), `ProjectQueryApiIntegrationTest`(공개 범위, 복수·무 주관 부서 공개 범위, 상세 404, 멤버·단계 API, 필터, 커서 경계)
- unit(FR-PRJ-07): `AiDraftTest`(DRAFT에서 한 번만 결정, FAILED·결정된 초안은 409), `WbsDraftValidatorTest`(tempId 중복, 참조, 기간·프로젝트 기간 없음·한쪽만 있음, 순환, 담당자), `WbsDraftAiClientTest`(aiserver 계약 변환, 기간 null·기존 작업 단계 null, 없는 단계 502), `NotReadyWbsDraftGeneratorTest`
- integration(FR-PRJ-07): `WbsAiDraftApiIntegrationTest`(ai_drafts DRAFT·FAILED·ACCEPTED·PARTIALLY_ACCEPTED 기록, 생성·반영·토큰 1회용·만료·다른 프로젝트 토큰, 검증 실패 후 같은 토큰으로 재반영, 멤버 아닌 담당자, 기간 없는 프로젝트, 단계 없음·WBS 기능 꺼짐 404, PM 아님 403, AI 사용 불가 503, 감사 로그)
- unit(FR-WBS-01): `WbsTaskTest`(상위 기간 산출·하위작업 없음, 같은 값 판정·공수 scale, 상태 유지), `WbsEditorTest`(역할별 허용·거부: PM, 개발자·QA 본인/남의 작업, 현업 담당자, 역할 없음), `WbsTaskRulesTest`(기간 경계·프로젝트 기간 없음/한쪽, 활성 멤버 담당자)
- integration(FR-WBS-01): `WbsTaskApiIntegrationTest`(3레벨 등록 후 재조회 유지, 조회 권한·404·기능 꺼짐·관리자, 쓰기 권한 매트릭스, 3단계 중첩 거부, 이동·승격·재조회·상위 기간 재산출·형제 version 유지, 같은 위치 재전송 멱등·낡은 version 409·위치 경계, 수정 409·상위 기간 직접 변경 400, 값 검증 경계·다른 프로젝트 작업 404, 항목 수 상한, 삭제 거부·재산출·감사 로그)
- unit(FR-WBS-03): `WbsDependencyCycleValidatorTest`(분기·합류 비순환 그래프, 자기 참조·간접 순환, 중복 간선)
- integration(FR-WBS-03): `TaskDependencyApiIntegrationTest`(전체 교체·해제·멱등, 지연 일수·후행 영향, 역할별 조회·변경 권한, 자기 참조·간접 순환·다른 프로젝트·없는 작업, 입력 개수·ID·기능 경계)
- unit(FR-TSK-01): `WbsTaskTest`(상세 기본값·수정·같은 값 판정, 완료 시각 기록·유지·취소, 유형·우선순위 필수), `TaskHistoryRecorderTest`(변경 없음·공수 scale 무시, 배정·해제, 여러 종류 동시 변경 순서·바뀐 필드만, 이동·상태, 삭제 전 값)
- integration(FR-TSK-01): `TaskApiIntegrationTest`(기본값·지정값 등록과 상세·트리·CREATED 이력, 조회 권한·비공개 404·다른 프로젝트 업무 404·기능 꺼짐·관리자, 수정 권한 매트릭스, DETAILS_CHANGED 전·후 값·같은 값 멱등·낡은 version 409·WBS와 version 공유, 경계값 2000/2001자·누락·허용 밖 값, 배정·변경·해제 이력·개발자 배정 403, 일정·상위 기간 산출·상세 이력·실패 시 이력 없음, 이동·삭제 후 이력 보존·JSONB 객체 저장, 커서 페이지·size 경계·잘못된 커서, V11 기본값·CHECK), `WbsAiDraftApiIntegrationTest.applyDerivesParentSchedule`(AI 반영 CREATED 이력)
- unit(FR-TSK-02): `BoardColumnTest`(기본 4개·라벨 키, WIP 경계: 한도와 같음/+1/없음/1), `BoardLayoutTest`(저장 컬럼·빈 컬럼·없는 컬럼 → 같은 상태 첫 컬럼, 카드 순서, 빈 보드), `WbsTaskTest`(컬럼 이동: 상태·변경 시각, 완료 시각·진척률 100, 같은 상태 컬럼, 완료 이탈), `TaskHistoryRecorderTest`(STATUS_CHANGED 상태·컬럼, BOARD_COLUMN_CHANGED, PROGRESS_CHANGED)
- integration(FR-TSK-02): `BoardApiIntegrationTest`(생성 시 기본 컬럼·단계 없음 프로젝트, 새 작업 배치, 조회·담당자 필터·카드 수, 조회 권한·비공개 404·칸반 꺼짐·관리자, 칸반만 켠 프로젝트(업무 상세 허용·WBS 404·빈 컬럼 작업), 이동 상태 동기화·완료·이력, 멱등·낡은 version 409·WBS와 version 공유, 이동 권한 매트릭스·IDOR 404·입력 400, WIP 경고 경계·필터 무관, 컬럼이 비어 있는 카드의 카드 수(첫 컬럼만)·WIP·삭제 판정, 컬럼 추가·변경·경계값(제어 문자 포함)·감사 로그, 컬럼 20개 상한, 순서 변경·잘못된 순서, 삭제 불변식, 관리 권한, 마일스톤 지연 경고 해제), `migration.V12BoardColumnsMigrationTest`(V11 데이터에 V12 적용: 백필·CHECK), `WbsAiDraftApiIntegrationTest.apply`(AI 반영 작업의 컬럼)
- integration(FR-PRJ-04): `ProjectMemberApiIntegrationTest`(초대·역할 변경·제외·이력), `ProjectMultiPmApiIntegrationTest`(PM 추가·동일 권한·해제·최소 1명·PM 제외·추가 규칙·위임 API 폐지·동시 해제 경쟁), `AdminProjectPmApiIntegrationTest`(재지정, 비활성 PM 전원 제외, 활성 PM 있으면 409, 새 PM 규칙, 관리자 전용), `common.web.CoreviaExceptionHandlerTest`(동시 수정 409)
- unit(FR-ADM-07): `CommonCodeTest`(라벨 키 규칙·최대 길이, 코드·그룹 코드 형식과 길이 경계)
- integration(FR-ADM-07): `AdminCodeApiIntegrationTest`(관리자 전용 404·401, 그룹·코드 목록, 코드 추가·수정·삭제 404, 정렬 순서 변경과 감사 로그 전·후, 본문의 코드 값·라벨 키·사용 여부 무시, 경계값 0/9999, 범위·누락 400, 없는 그룹·코드 404, 같은 값 재전송 무변경), `CodeApiIntegrationTest`(로그인 사용자 조회, 사용 중 코드만·정렬, 관리자 정렬 변경 즉시 반영, 빈 그룹·없는 그룹·형식 오류·비로그인), `code.CodeSeedConsistencyTest`(TASK_STATUS·PROJECT_STATUS 시드 = enum 값·순서, 라벨 키 규칙)
- unit(FR-ADM-06): `LabelLanguageTest`(태그 해석·간체 거부·길이 경계, lang → Accept-Language → ko 순서, 가중치·q=0·형식 오류·과도한 길이), `I18nLabelTest`(정규화·누락 한국어 대체·변경 감지·라벨 키/문구 형식과 길이 경계), `AdminLabelServiceLikePatternTest`(LIKE 이스케이프)
- integration(FR-ADM-06): `AdminLabelApiIntegrationTest`(관리자 전용 404·401, 등록·정규화·감사 로그, 중복 409·형식 400·경계값 100/500자, 수정·언어별 이력, 같은 값 재전송, 옛 version 409, 키 변경·삭제 불가, 커서·키/문구 검색·와일드카드 이스케이프, 누락 필터·잘못된 필터), `LabelApiIntegrationTest`(로그인 없이 조회, 언어 결정 순서, 누락 한국어 대체, 수정 즉시 반영, 비밀번호 변경 강제 세션 허용, 위조 쿠키, Vary 헤더), `LabelSeedConsistencyTest`(코드 라벨 키 1:1·시드 번역)
- unit(FR-ADM-09·NFR-06): `SensitiveValueMaskerTest`(패턴별 마스킹·키 이름 유지, 가릴 것 없는 문장·스택 프레임·고객사·공정 문구 무변경, 숫자 경계, 여러 Caused by, 20만 자 악의적 입력 5초 제한), `ErrorLogTest`(요청 정보·원인 스택, null 값, 가린 뒤 자르기·2000자 경계, 원인 단계·프레임 제한·순환), `AdminErrorLogServicePrefixPatternTest`
- integration(FR-ADM-09): `AdminErrorLogApiIntegrationTest`(관리자 전용 404·401, 미처리 예외 500 공통 응답·내부 정보 미노출·요청 정보·마스킹·쿼리/헤더 미저장, checked 예외, 업무 롤백과 기록 분리, 업무 예외·입력 오류·권한·매핑 없는 경로·415 미기록, 최신순 커서·목록 스택 제외·상세·404, 기간·유형·경로 필터와 경계·400), `ErrorLogRecordFailureTest`(기록 실패 시 같은 500). 예외는 테스트 전용 `support.ErrorTriggerTestController`(/api/test-errors)로 일으킨다
- 미검증: 같은 사용자 동시 초대·같은 코드 동시 생성의 유니크 제약 경쟁은 DB 제약으로 막지만 자동 테스트는 없다. H2로 검증하므로 PostgreSQL 고유 동작(잠금 대기 시간 등)은 운영 DB에서 확인이 필요하다.
- 테스트 공통(`src/test/integration`): `support.ApiIntegrationTestSupport`(로그인·요청·오류 검증), `support.DatabaseCleaner`(테스트 클래스 간 공유 H2를 FK 역순으로 비움. 테이블을 추가하면 여기도 추가)
- 미검증: `RedisAuthSessionStore`·`RedisWbsDraftTokenStore`는 실제 Redis가 필요해 자동 테스트가 없다(Testcontainers 도입 시 추가). aiserver의 실제 WBS 생성은 FastAPI 이식 후 테스트한다.
- CI gate: `.github/workflows/DevelopCI.yml`이 PR마다 `./gradlew clean check bootJar`(단위 + 통합 테스트)를 실행한다.

## 11. Build·배포·운영

- `Dockerfile`(multi-stage, JRE 17, non-root) → `compose.yaml`로 BE + Redis 기동. 운영 배포 방식은 NFR-16 배포 가이드 수령 후 확정.
- BE 포트는 nginx 뒤에서만 접근하게 둔다. `server.forward-headers-strategy=framework`로 `X-Forwarded-For`를 접속 IP로 쓰므로, BE 포트를 외부에 직접 열면 IP를 위조할 수 있다.
- graceful shutdown: `server.shutdown=graceful`, 30초.

## 12. 주요 Dependency

| Dependency | 목적 |
|---|---|
| spring-boot-starter-security | 인증·인가 체인 |
| spring-boot-starter-data-redis | 세션 저장소, AI 초안 토큰 |
| spring-boot-starter-flyway, flyway-database-postgresql | 스키마 migration |
| io.jsonwebtoken:jjwt 0.13.0 | JWT 발급·검증 (Apache-2.0) |
| com.h2database:h2 (test) | 테스트 DB |

- Gradle dependency lockfile은 아직 없다(도입 필요, security 가이드 MUST).

## 13. License

- 미정 (사내 프로젝트).

## 14. 개인 TODO (SCRUM-111)

- 기준: FR-PSN-01·권한 매트릭스 3.3. 최초 구현은 원격 `develop`의 `6d39260`, 이후 `14b1322` 병합 검증은 [기록](docs/reviews/SCRUM-111-develop-merge-review.md)을 따른다.
- 개인 TODO 등록·상세·제목/마감/완료 변경·소프트 삭제와 배정 WBS 실시간 통합 조회를 구현했다. 사용자 결정에 따라 승인 요청 원천은 후속 연결, 삭제는 소프트 삭제, 마감 임박 기본값은3일이다. 이 BE 변경이 승인 원천 기능·FE 화면까지 구현한 것은 아니다.
- API 계약(PR 전): [06. 개인 업무](docs/api/06-personal-todos-draft.md) · [Confluence API](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/21561547). ERD 보완: [개인 TODO](docs/erd/06-personal-todos-draft.md), 시퀀스: [생성·조회·소프트 삭제](docs/sequence/06-personal-todos-draft.md), [최신 보안·검수·검증 한계](docs/reviews/SCRUM-111-soft-delete-review.md). 이전 초안 검수 기록은 당시 결과를 보존한다.
- 경로: `GET/POST /api/me/todos`, `GET/PATCH/DELETE /api/me/todos/{todoId}`. 소유자는 로그인 주체에서만 정한다. 기본 목록은 미삭제·미완료 개인 TODO·현재 담당 업무, 완료 개인은 `done=true`. `dueSoonDays` 기본3·명시1~365, `size` 기본20·최대100, 불투명 cursor를 사용한다.
- 날짜: `Asia/Seoul`의 오늘을 기준으로 오늘→임박→지연→이후→마감없음 정렬, 지연은 `overdue`로 표시한다. 커서를 이어가는 동안 정렬 날짜를 고정하고 권한·행 상태는 매번 새로 확인한다.
- 등록은 클라이언트 UUID `requestId`가 필수다. 본인 범위 같은 키·같은 입력의 재전송은 처음 응답, 다른 입력은409. 현재 할 일 내용을 바꿔도 최초 요청 해시를 보존한다.
- DELETE는 본인 항목에 `deleted_at`을 기록하고 최소 정보의 `PERSONAL_TODO_DELETED` 감사 로그를 같은 트랜잭션에 남긴다. 최초·재요청 모두204, 감사는 최초1건이다. 타인·없는 항목과 삭제 후 GET/PATCH는404다. 삭제행의 생성 키는 보존하며 같은 생성 요청 재전송은409 `TODO_DELETED`로 부활·복제를 막는다.
- 삭제행·멱등키의 보존 기간·물리 purge·복구는 운영 결정 미정이며 자동 작업을 도입하지 않았다. 승인 원천은 사용자 승인 후속 범위다. FE는 배정 후1분 이내 표시를 위해1분 이내 재조회해야 한다.
- Migration V13은 `personal_todos`만 추가한다. **운영 V8~V12 예약 작업을 통합·승인하고 순서대로 적용한 뒤 V13을 migrate해야 한다. V13 단독 선적용 금지.** 기본 Flyway outOfOrder=false에서 뒤늦은 V8~V12는 적용에 실패하므로 번호 변경·outOfOrder 임의 활성화로 우회하지 않는다.
- V14는 V13을 수정하지 않고 nullable `deleted_at`·조회 인덱스를 추가한다. V8~V12 → V13 → V14 → 새 API 순서로 배포한다. 구 API는 삭제행을 필터하지 않으므로 삭제 활성화 후 구/신 API 혼용·구 리더로 rollback하지 않는다. [소프트 삭제 보안·검수 기록](docs/reviews/SCRUM-111-soft-delete-review.md) 참고.
- 기존 표준 테스트 명령은 그대로다. 현재 오프라인 환경에는 JaCoCo agent0.8.15가 없어 표준 test/check·커버리지 증거를 확보하지 못했다. 별도 작업 공간 init으로 instrumentation만 끈 대체 테스트·빌드 증거와 기존 formatter/lint/lock/security-scan 미구축 한계는 검수 문서에 기록한다.
