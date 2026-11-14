# CoreviaAI_BE

> 상태: Draft
> 담당자: 미정 (팀에서 지정 필요)
> 마지막 검토일: 2026-11-07
> 적용 범위: `CoreviaAI_BE/` 백엔드 (Spring Boot)

## 1. 프로젝트 개요

- 목적: CoreviaAI(원익큐앤씨 전사 AI 협업관리 웹앱)의 백엔드 API. 요구사항 기준은 Confluence [요구사항 명세서 (전사관리)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909)다.
- 구현된 기능
  - FR-ADM-01 로그인·계정 (Jira SCRUM-96): ID/PW 로그인·로그아웃, 토큰 재발급, 내 계정 조회, 비밀번호 변경, 관리자 비밀번호 초기화, 5회 연속 실패 잠금, 30분 무활동 세션 만료, 로그인 시도 기록
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
│  └─ security                       AuthServerSecurityConfig, AuthSessionLoginResolver
├─ coreviaai_be/                     업무 모듈 (프로젝트·WBS 등, 이후 기능부터 추가)
│  └─ security                       CoreviaSecurityConfig
└─ global/                           공통. authserver·coreviaai_be를 import하지 않는다
   ├─ user                           User(users): 사람·조직 정보. authserver·업무 모듈이 함께 사용
   ├─ jwt                            JwtTokenProvider, AuthCookieManager, JwtProperties, LoginUser(principal)
   ├─ security                       JwtAuthenticationFilter, LoginSessionResolver(인터페이스), ApiSecuritySupport
   ├─ common/type                    SystemRole
   ├─ common/entity                  BaseTimeEntity (createdAt·updatedAt, UTC Instant)
   ├─ common/exception               ErrorCode, CustomException, GlobalExceptionHandler
   ├─ common/response                ApiResponse
   └─ config                         Clock, JPA auditing, PasswordEncoder, 401/403 JSON 응답
```

- 의존 방향: `authserver → global`, `coreviaai_be → global`. 업무 코드는 인증 결과를 `@AuthenticationPrincipal LoginUser`(global)로만 받고 `authserver`를 import하지 않는다. 그래서 SSO로 바뀌면 `authserver`만 교체하면 된다.
- 인증 방식 분리는 두 단계로 되어 있다: 패키지 단위(`authserver` 교체)와, 같은 서버 안에서 자격 증명 확인만 바꾸는 `CredentialAuthenticator` 구현체 교체.
- 보안 체인은 경로를 소유한 모듈이 갖는다(WebUI_BE ADR-0007 방식).
  - `@Order(1)` `authserver.security.AuthServerSecurityConfig`: `/api/auth/**`, `/api/users/me/password`, `/api/admin/users/*/password-reset`
  - `@Order(2)` `coreviaai_be.security.CoreviaSecurityConfig`: 나머지 `/api/**` (로그인 필수, `/api/admin/**`은 시스템 관리자)
  - 마지막 `global.config.security.WebSecurityConfig`: 위에 속하지 않는 경로는 모두 거부(`/error`만 허용)
  - 공통 설정과 JWT 필터는 `global.security`에 있고, 세션 확인은 `LoginSessionResolver` 인터페이스로만 한다. 구현(`AuthSessionLoginResolver`, Redis 세션)은 authserver에 있어 SSO 교체 시 이것만 바꾸면 업무 체인은 그대로 쓴다.
- 사용자와 로그인 정보 분리: 사람·조직 정보(`users`, `global.user`)와 로컬 ID/PW 로그인 정보(`local_credentials`, `authserver.domain.credential`)를 나눴다. `LocalCredential`은 `User`와 JPA 연관관계 없이 `user_id`만 갖는다. SSO로 바꾸면 `local_credentials` 테이블과 `authserver`만 제거하고, `users`와 이를 참조하는 업무 데이터는 그대로 둔다. FR-ADM-02의 사용자 추가는 `users` 생성(업무 모듈) + 임시 비밀번호 발급(authserver 관리자 초기화 API) 두 단계가 된다.
- ADR: 아직 없음. 세션+JWT 인증 방식과 이 패키지 구성은 ADR로 남길 대상이다(팀 결정 필요).

## 6. API

- API 명세 위치: **미정** (Notion 또는 Excel 위치를 팀에서 지정 예정. 지정 전에는 이 절을 참고).
- 응답 형식: `{"success": bool, "response": ..., "error": {"message": "...", "status": 400}}`

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | 불필요 | `{loginId, password}` → 쿠키 발급, 계정 정보 응답 |
| POST | `/api/auth/refresh` | RefreshToken 쿠키 | AccessToken 재발급 |
| POST | `/api/auth/logout` | 불필요(있으면 세션 종료) | 세션 삭제, 쿠키 만료 |
| GET | `/api/auth/me` | 필요 | 내 계정 정보 (`mustChangePassword` 포함) |
| PATCH | `/api/users/me/password` | 필요 | `{currentPassword, newPassword}` → 성공 시 모든 세션 종료, 재로그인 |
| POST | `/api/admin/users/{userId}/password-reset` | SYSTEM_ADMIN | 임시 비밀번호 발급(응답에 1회), 잠금 해제, 대상 사용자 세션 종료 |

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
| 권한 없음 (/api/admin/**) | 404 | 존재 비노출 |

### 로그인 규칙

- 없는 ID와 틀린 비밀번호는 같은 400 오류로 응답한다(계정 존재 비노출). 로그에는 구분해 남긴다.
- 5회 연속 실패 시 잠금(423). 성공하면 연속 실패 횟수 초기화. 잠금은 관리자 비밀번호 초기화로만 풀린다.
- 비활성 계정은 비밀번호가 맞을 때 403.
- 비밀번호 규칙: 영문·숫자·특수문자 각 1자 이상, 8~64자, 공백·비ASCII 불가. 대문자 필수 아님.
- CSRF: 토큰 대신 SameSite=Lax 쿠키로 막는다. FE와 BE가 nginx로 같은 origin에서 서비스된다는 전제이며, origin이 달라지면 재검토해야 한다. CORS는 설정하지 않았다(same-origin).
- pagination·idempotency: 이 기능에는 해당 없음(목록 API 없음, 로그인은 재시도해도 새 세션만 생김).

## 7. Database와 Migration

- 저장소: PostgreSQL (`users`, `local_credentials`, `login_history`), Redis (세션).
- 시각은 모두 UTC instant(`timestamptz`)로 저장한다. KST 표시는 응답·화면 경계에서 변환한다.
- 동시성: 로그인 시 `users` → `local_credentials` 순서로 `SELECT … FOR UPDATE`해 같은 계정의 동시 로그인과 실패 횟수 갱신을 순서대로 처리한다. 비밀번호 변경·초기화는 `local_credentials`만 잠근다. 두 테이블 모두 `version`으로 낙관적 잠금도 건다.
- Migration: `src/main/resources/db/migration/V{n}__설명.sql`, 기동 시 Flyway 자동 적용. 이미 적용된 파일은 수정하지 않고 새 버전을 추가한다(roll-forward).
- backup·보존: DB 백업은 NFR-13(일 1회) 기준으로 회사 DB 측 운영. `login_history` 1년 보관 정리 작업은 FR-ADM-04에서 구현 예정.

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
- 민감 정보: 비밀번호(해시만 저장), JWT 서명 키, 로그인 IP(개인정보). 분류 기준·로그 접근 권한: 미정.
- secret rotation·incident 대응 절차: 미정.

## 10. 테스트

```
./gradlew test
```

- unit: `PasswordPolicyTest`, `LocalCredentialTest`(잠금·초기화), `JwtTokenProviderTest`(만료·위조·용도), `TemporaryPasswordGeneratorTest`, `InitialAdminInitializerTest`
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
