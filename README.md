# CoreviaAI_BE

> 상태: Draft
> 담당자: 미정 (팀에서 지정 필요)
> 마지막 검토일: 2026-11-12
> 적용 범위: `CoreviaAI_BE/` 백엔드 (Spring Boot)

## 1. 프로젝트 개요

- 목적: CoreviaAI(원익큐앤씨 전사 AI 협업관리 웹앱)의 백엔드 API. 요구사항 기준은 Confluence [요구사항 명세서 (전사관리)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909)다.
- 구현된 기능
  - FR-ADM-01 로그인·계정 (Jira SCRUM-96): ID/PW 로그인·로그아웃, 토큰 재발급, 내 계정 조회, 비밀번호 변경, 관리자 비밀번호 초기화, 5회 연속 실패 잠금, 30분 무활동 세션 만료, 로그인 시도 기록
  - FR-ADM-02 사용자·조직 관리 (Jira SCRUM-97): 사용자 추가·수정·활성/비활성, 시스템 역할 변경, 프로젝트 생성 권한 부여·회수, 부서(계층) 관리, 변경 이력(감사 로그) 조회
  - FR-PRJ-01 프로젝트 생성 (Jira SCRUM-98): 단계형 생성(기본 정보 → 운영 방식 → 액터 → 멤버(선택) → 게이트 승인자(선택)), 생성자 PM 자동 지정, 폭포수 5단계·게이트 자동 생성, 공개 범위에 따른 목록(필터·커서)·상세 조회. MVP는 폭포수·SW 액터만 선택 가능
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
./gradlew test       # 성공 기준: BUILD SUCCESSFUL, 실패 테스트 0건
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
- 테스트 설정은 `src/test/resources/application-test.properties` (테스트 전용 키, 운영 비밀값 아님).
- 운영 secret 관리·rotation 책임: 미정. 배포 가이드(NFR-16) 수령 후 결정 필요. `JWT_SECRET`을 바꾸면 발급된 모든 토큰이 무효가 되어 전원 재로그인한다.

### 최초 관리자 계정

비밀번호를 코드·SQL에 남기지 않기 위해, 시스템 관리자가 한 명도 없을 때만 `INITIAL_ADMIN_*` 값으로 첫 관리자를 만든다. 이 계정은 첫 로그인 후 비밀번호를 바꿔야 다른 기능을 쓸 수 있다. 관리자가 이미 있으면 값이 남아 있어도 무시한다.

## 5. 구조와 Architecture

WebUI_BE의 `authserver / erpweb / global` 구성을 따른다. 인증 서버(`authserver`)는 이후 사내 SSO로 교체될 수 있으므로 업무 모듈과 분리한다.

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
├─ coreviaai_be/                     업무 모듈
│  ├─ domain/department              Department(departments): 부서 계층(parentId). 사용자 소속·이후 프로젝트 주관 부서의 기준
│  ├─ domain/project                 프로젝트 (FR-PRJ-01)
│  │  ├─ controller                  ProjectController
│  │  ├─ service                     ProjectCreationService(생성), ProjectQueryService(목록·상세, 공개 범위)
│  │  ├─ policy                      ProjectCreationPolicy(생성 규칙), ProjectVisibilityPolicy(공개 범위·경영진 소속 범위)
│  │  ├─ structure                   ProjectStructureInitializer(운영 방식별 기본 구조) + WaterfallStructureInitializer(5단계·게이트)
│  │  ├─ entity                      Project, ProjectActorAssignment, ProjectMember, ProjectPhase, type/(enum)
│  │  └─ dto                         request / response
│  ├─ domain/admin                   관리자 콘솔 (FR-ADM-02)
│  │  ├─ controller                  AdminUserController, AdminDepartmentController, AdminExceptionHandler(동시 수정 409)
│  │  ├─ service                     AdminUserService, AdminDepartmentService
│  │  │  ├─ command                  서비스 입력 (요청 DTO와 분리)
│  │  │  └─ result                   서비스 결과 (응답 DTO는 컨트롤러에서 변환)
│  │  └─ dto                         request / response
│  └─ security                       CoreviaSecurityConfig
└─ global/                           공통. authserver·coreviaai_be를 import하지 않는다
   ├─ user                           User(users): 사람·조직 정보(소속 부서·직무·프로젝트 생성 권한 포함). authserver·업무 모듈이 함께 사용
   ├─ audit                          AuditLog(audit_logs), AuditLogger, AuditActor: 감사 로그. 변경과 같은 트랜잭션에서 기록
   ├─ jwt                            JwtTokenProvider, AuthCookieManager, JwtProperties, LoginUser(principal)
   ├─ security                       JwtAuthenticationFilter, LoginSessionResolver·UserSessionTerminator(인터페이스), ApiSecuritySupport
   ├─ common/type                    SystemRole
   ├─ common/entity                  BaseTimeEntity (createdAt·updatedAt, UTC Instant)
   ├─ common/exception               ErrorCode, CustomException, GlobalExceptionHandler
   ├─ common/response                ApiResponse
   ├─ common/pagination              CursorCodec, CursorPageRequest, CursorPage (커서 목록)
   └─ config                         Clock, JPA auditing, PasswordEncoder, 401/403 JSON 응답, AuditActor 인자 주입(web)
```

- 의존 방향: `authserver → global`, `coreviaai_be → global`. 업무 코드는 인증 결과를 `@AuthenticationPrincipal LoginUser`(global)로만 받고 `authserver`를 import하지 않는다. 그래서 SSO로 바뀌면 `authserver`만 교체하면 된다.
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
| PATCH | `/api/admin/users/{userId}/system-role` | SYSTEM_ADMIN | 역할 변경 + 대상 세션 종료. USER가 아니게 되면 프로젝트 생성 권한 회수 |
| PATCH | `/api/admin/users/{userId}/status` | SYSTEM_ADMIN | 활성·비활성. 비활성 시 대상 세션 종료 |
| PATCH | `/api/admin/users/{userId}/project-creation` | SYSTEM_ADMIN | 프로젝트 생성 권한 부여·회수(USER만) |
| GET | `/api/admin/users/{userId}/histories` | SYSTEM_ADMIN | 변경 이력(감사 로그, 커서) |
| GET | `/api/admin/departments` | SYSTEM_ADMIN | 부서 목록(커서, id 오름차순). `cursor`, `size`, `status` |
| POST | `/api/admin/departments` | SYSTEM_ADMIN | 부서 추가 |
| PATCH | `/api/admin/departments/{departmentId}` | SYSTEM_ADMIN | 이름 변경·상위 부서 이동 |
| PATCH | `/api/admin/departments/{departmentId}/status` | SYSTEM_ADMIN | 활성·비활성 |
| GET | `/api/projects/creation-options` | USER·EXECUTIVE | 생성 화면 ②·③ 선택지(운영 방식·부서 유형별 액터, `enabled`·`required`) |
| GET | `/api/projects/code-availability` | 생성 권한자 | `?code=` 코드 중복 확인(대문자로 정규화한 코드와 `available`) |
| POST | `/api/projects` | 생성 권한자 | 프로젝트 생성. 생성자 PM 자동 지정, 폭포수 5단계·게이트 생성, 감사 로그(`PROJECT_CREATED`). 응답은 상세 |
| GET | `/api/projects` | USER·EXECUTIVE | 프로젝트 목록(커서, id 내림차순). `cursor`, `size`, `status`, `departmentId`, `pmUserId`. 공개 범위 밖은 제외 |
| GET | `/api/projects/{projectId}` | USER·EXECUTIVE | 상세(운영 방식·지정 액터·멤버·단계). 볼 수 없으면 404 |

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
- 프로젝트 생성 권한은 시스템 관리자가 일반 사용자(USER)에게만 부여한다.
- 본인 계정의 역할·상태는 바꿀 수 없고, 마지막 활성 시스템 관리자는 강등·비활성화할 수 없다.
- 부서 이름은 같은 상위 부서 안에서 중복 불가(최상위끼리 포함). 자기 자신·하위 부서 밑으로 이동 불가. 활성 소속 인원이나 활성 하위 부서가 있으면 비활성화 불가.
- 사용자·부서는 삭제하지 않고 비활성화한다. 모든 변경과 관리자 비밀번호 초기화는 `audit_logs`에 변경 전·후 값·변경자·IP와 함께 남는다(임시 비밀번호는 기록하지 않음).

### 프로젝트 생성·조회 규칙 (FR-PRJ-01)

- 생성은 '프로젝트 생성 권한'이 있는 활성 일반 사용자만 할 수 있고(403 `PROJECT_CREATION_FORBIDDEN`), 생성자가 PM이 된다. `projects.pm_user_id`(현재 PM, FR-PRJ-04 위임으로 바뀜)와 `created_by`(최초 생성자)를 나눠 저장한다.
- 단계형 화면의 입력은 FE가 모아 마지막에 한 번 `POST /api/projects`로 보낸다. 한 트랜잭션으로 프로젝트·액터·멤버·단계를 만들어, 반쯤 만든 프로젝트가 남지 않는다.
- 필수: 이름(100자 이하), 코드, 기간(시작일 ≤ 종료일, 과거 허용), 주관 부서(활성), 공개 범위, 운영 방식, 부서 유형, 액터. 코드는 영문·숫자로 시작하는 영문·숫자·하이픈 2~20자, 대문자로 저장한다(대소문자만 다르면 중복).
- MVP 선택 범위: 운영 방식은 폭포수만, 부서 유형은 SW만, 액터는 PM·현업 담당자(`BUSINESS_OWNER`)·개발자·QA만. 비활성 값은 FE가 막더라도 서버에서 다시 거부한다. 활성 여부는 `Methodology`·`DepartmentType` enum의 `enabled`가 기준이라, 애자일(FR-PRJ-10)·제조 모듈(4.14)을 열 때 enum 값만 바꾸면 선택지 API와 검증이 함께 바뀐다.
- 액터: PM은 필수, 나머지는 선택. 초대 멤버의 역할은 지정한 액터 중에서만 고르고 PM 역할은 줄 수 없다. 한 사람은 한 프로젝트에서 역할 하나.
- 초대 대상: 활성 일반 사용자(다른 부서 포함). 생성자 본인, 경영진(프로젝트 읽기 전용), 시스템 관리자, 비활성·없는 사용자는 거부.
- 게이트 승인자(선택): 초대한 멤버 중 PM이 아닌 1명. 5개 단계 게이트 모두에 적용된다. 이후 지정·변경과 미지정 시 단계 전환 차단은 FR-PRJ-05 범위.
- 공개 범위: 전사 공개는 전원, 부서 공개는 주관 부서 소속자(하위 부서 제외)와 멤버, 멤버 전용은 멤버만 본다. 경영진은 본인 부서와 모든 하위 부서가 주관하는 프로젝트를 멤버 전용까지 보고, 그 밖은 전사 공개만 본다. 상세에서 볼 수 없는 프로젝트는 404로 존재를 드러내지 않는다.
- 목록 정렬은 최신 생성순 하나만 제공한다. 종료일·이름순 정렬과 리스크 등급 필터(FR-AIR-06)는 후속.
- 프로젝트 상태(`PLANNED`·`IN_PROGRESS`·`ON_HOLD`·`COMPLETED`·`CANCELLED`, 생성 시 `PLANNED`)는 명세에 정의가 없어 팀 확정 전 초안이다. 상태 변경 API는 없다.

### 목록 페이지네이션 (커서)

- 요청: `?cursor={이전 응답의 nextCursor}&size=20` (기본 20, 최대 100). 첫 페이지는 `cursor` 없이 요청한다.
- 응답: `{"items": [...], "nextCursor": "..." | null, "hasNext": true | false}`
- 정렬은 API마다 고정한다: 사용자 목록·변경 이력·프로젝트 목록은 id 내림차순(최신순), 부서 목록은 id 오름차순(상위 부서가 먼저 와서 트리 구성이 쉬움).
- 커서는 불투명 문자열이며 형식이 틀리면 400 `INVALID_CURSOR`. size+1개를 읽어 다음 페이지 여부를 판단한다(count 쿼리 없음).

### 중복 요청(idempotency)

- 사용자 추가: `loginId`가 자연 키다. 같은 요청을 다시 보내면 409 `DUPLICATE_LOGIN_ID`로 끝나고 계정은 하나만 생긴다. 별도 Idempotency-Key는 쓰지 않는다.
- 부서 추가: (상위 부서, 이름)이 자연 키다. 재전송은 409 `DUPLICATE_DEPARTMENT_NAME`.
- 프로젝트 생성: `code`가 자연 키다. 재전송은 409 `DUPLICATE_PROJECT_CODE`로 끝나고 프로젝트는 하나만 생긴다(사전 검사 + `uk_projects_code`).
- 수정·상태 변경(PATCH): 현재와 같은 값이면 변경·이력 없이 현재 상태를 돌려준다. 역할 변경·비활성화는 DB 커밋 후 세션을 끊는데, 세션 저장소 장애로 실패하면 오류가 반환되고 같은 요청을 다시 보내면 세션 끊기만 다시 수행된다.

## 7. Database와 Migration

- 저장소: PostgreSQL (`users`, `local_credentials`, `login_history`, `departments`, `audit_logs`, `projects`, `project_actors`, `project_members`, `project_phases`), Redis (세션). 테이블 정의는 Confluence [BE_ERD](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/folder/12878096) 도메인 페이지 참고. ERD 원본은 `docs/erd/`(draw.io, 현재 `02-admin.drawio`).
- 시각은 모두 UTC instant(`timestamptz`)로 저장한다. KST 표시는 응답·화면 경계에서 변환한다.
- 동시성: 로그인 시 `users` → `local_credentials` 순서로 `SELECT … FOR UPDATE`해 같은 계정의 동시 로그인과 실패 횟수 갱신을 순서대로 처리한다. 비밀번호 변경·초기화는 `local_credentials`만 잠근다. 두 테이블 모두 `version`으로 낙관적 잠금도 건다.
- 동시성(관리자 기능): `users`·`departments`는 `version` 낙관적 잠금으로 동시 수정을 막고 충돌 시 409 `CONCURRENT_MODIFICATION`. 마지막 관리자 보호는 활성 관리자 행을 `FOR UPDATE`로 잠근 뒤 확인한다. 아이디·부서 이름 중복은 사전 검사 + DB 유니크 제약으로 막는다(최상위 부서 이름은 NULL 특성상 사전 검사만).
- V2(`V2__create_departments_and_audit_logs.sql`): `departments`, `audit_logs` 생성, `users`에 NULL 허용·기본값 컬럼만 추가해 기존 행과 V1 기준 코드에 영향 없음(expand). 되돌릴 때는 추가 컬럼·테이블을 지우는 새 버전을 추가한다.
- V3(`V3__create_projects.sql`): `projects`, `project_actors`, `project_members`, `project_phases` 테이블만 새로 만든다(기존 테이블 변경 없음). 프로젝트 기간은 달력 날짜라 `DATE`로 저장한다(UTC 시각 아님). enum 컬럼은 `audit_logs.action`과 같이 CHECK 제약 없이 이름을 저장한다. 되돌릴 때는 네 테이블을 지우는 새 버전을 추가한다.
- Migration: `src/main/resources/db/migration/V{n}__설명.sql`, 기동 시 Flyway 자동 적용. 이미 적용된 파일은 수정하지 않고 새 버전을 추가한다(roll-forward).
- backup·보존: DB 백업은 NFR-13(일 1회) 기준으로 회사 DB 측 운영. `login_history`·`audit_logs` 1년 보관(NFR-07) 정리 작업은 FR-ADM-04에서 구현 예정. `audit_logs`는 추가만 하고 수정·삭제 API가 없다.

## 8. 외부 연동

| 대상 | timeout | retry | 실패 시 |
|---|---|---|---|
| Redis (세션) | connect 2s, command 2s | 없음 (인증 판정을 추측으로 대신하지 않음) | 503 `외부 서비스에 일시적으로 연결할 수 없습니다` |
| PostgreSQL | HikariCP 기본값 | 없음 | 500 |

- timeout 값 근거: 세션 조회는 모든 요청 경로에 있어 NFR-01(2초 응답)을 넘기지 않도록 2초로 정했다. overall deadline은 아직 따로 두지 않았다(미정).
- test 대체: `InMemoryAuthSessionStore`(test 소스), H2.

## 9. 오류 처리와 관측성

- 오류 응답은 `ErrorCode` + `ApiResponse.error`. stack trace·내부 정보는 응답에 넣지 않는다.
- 비밀번호·토큰은 로그에 남기지 않는다(요청 DTO `toString` 마스킹, 임시 비밀번호 비로깅).
- 미정: correlation ID, metric·trace, SLO·alert, health check(actuator 미도입), 로그 보존 기간·접근 권한, incident runbook. 운영 배포 전 결정 필요.

## 9-1. 문서와 보안 운영 메타데이터

- 문서 담당자·검증일: 상단 참고 (담당자 미정).
- 민감 정보: 비밀번호(해시만 저장), JWT 서명 키, 로그인 IP·감사 로그 IP(개인정보). 감사 로그 details에는 비밀값을 넣지 않는다. 분류 기준·로그 접근 권한: 미정.
- secret rotation·incident 대응 절차: 미정.

## 10. 테스트

```
./gradlew test
```

- unit: `PasswordPolicyTest`, `LocalCredentialTest`(잠금·초기화), `JwtTokenProviderTest`(만료·위조·용도), `TemporaryPasswordGeneratorTest`, `InitialAdminInitializerTest`
- unit(FR-ADM-02): `UserTest`(역할 변경 시 권한 회수 등), `CursorCodecTest`, `CursorPageTest`, `AuditChangesTest`
- integration(FR-ADM-02): `AdminUserApiIntegrationTest`(추가→비밀번호 발급→로그인·초기화 감사 로그, 중복 재전송, 부서 필수·비활성 부서, 비활성화 로그인 차단·세션 종료, 역할 변경·세션 종료, 본인 변경 금지, 마지막 관리자 보호, 프로젝트 생성 권한, 이력, 커서 목록·필터·잘못된 커서), `AdminDepartmentApiIntegrationTest`(계층, 이름 중복, 순환 이동, 사용 중 비활성화, 커서 목록·경계)
- 미검증(FR-ADM-02): 낙관적 잠금 충돌(409)과 세션 저장소 장애 시 재전송 동작은 자동 테스트가 없다.
- unit(FR-PRJ-01): `ProjectCreationPolicyTest`(기간, 비활성 액터, PM 필수, 멤버·게이트 승인자 규칙)
- integration(FR-PRJ-01): `ProjectCreateApiIntegrationTest`(생성자 PM·5단계·승인자 적용·감사 로그, 멤버·승인자 생략, 코드 정규화·중복 재전송, 필수 항목·형식, 비활성 운영 방식·부서 유형·액터, 멤버·승인자 규칙, 생성 권한·시스템 관리자 차단, 비활성 부서, 코드 중복 확인, 선택지), `ProjectQueryApiIntegrationTest`(일반 사용자·경영진 공개 범위, 상세 404, 필터, 커서 경계)
- 미검증(FR-PRJ-01): 사전 중복 검사와 저장 사이에 같은 코드가 동시에 들어오는 경쟁은 DB 유니크 제약으로 막지만 자동 테스트는 없다.
- 테스트 공통: `support.ApiIntegrationTestSupport`(로그인·요청·오류 검증), `support.DatabaseCleaner`(테스트 클래스 간 공유 H2를 FK 역순으로 비움. 테이블을 추가하면 여기도 추가)
- integration: `AuthApiIntegrationTest` (MockMvc + H2 + 메모리 세션 저장소 + 시간 이동 가능한 Clock). 미인증 차단, 로그인 성공·실패 기록, 5회 잠금, 비활성 계정, 30분 무활동 만료·연장, 로그아웃, 비밀번호 변경, 관리자 초기화 흐름, 로컬 로그인 정보가 없는 사용자, 모듈별 보안 체인.
- 미검증: `RedisAuthSessionStore`는 실제 Redis가 필요해 자동 테스트가 없다(Testcontainers 도입 시 추가).
- CI gate: 미구성.

## 11. Build·배포·운영

- `Dockerfile`(multi-stage, JRE 17, non-root) → `compose.yaml`로 BE + Redis 기동. 운영 배포 방식은 NFR-16 배포 가이드 수령 후 확정.
- BE 포트는 nginx 뒤에서만 접근하게 둔다. `server.forward-headers-strategy=framework`로 `X-Forwarded-For`를 접속 IP로 쓰므로, BE 포트를 외부에 직접 열면 IP를 위조할 수 있다.
- graceful shutdown: `server.shutdown=graceful`, 30초.

## 12. 주요 Dependency

| Dependency | 목적 |
|---|---|
| spring-boot-starter-security | 인증·인가 체인 |
| spring-boot-starter-data-redis | 세션 저장소 |
| spring-boot-starter-flyway, flyway-database-postgresql | 스키마 migration |
| io.jsonwebtoken:jjwt 0.13.0 | JWT 발급·검증 (Apache-2.0) |
| com.h2database:h2 (test) | 테스트 DB |

- Gradle dependency lockfile은 아직 없다(도입 필요, security 가이드 MUST).

## 13. License

- 미정 (사내 프로젝트).
