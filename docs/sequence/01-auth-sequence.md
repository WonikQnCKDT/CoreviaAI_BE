<!-- Confluence Sequence 본문과 같은 판정 순서. develop 50f96d2076b09500c986c684d0468f430a00601e. draw.io·PNG는 이 파일에 넣지 않았고, 첨부 완료로 보지 않는다. -->

## 01-1. 로그인
API: `POST /api/auth/login`
- 참여자: FE, AuthController, AuthService, LocalCredentialAuthenticator, PostgreSQL, Redis

### 핵심 포인트
- 자격 증명 확인은 하나의 DB 트랜잭션`users` → `local_credentials` 순서로 `FOR UPDATE` 잠금 (동시 로그인·실패 횟수 정확성, 교착 방지)
- 실패도 예외 대신 결과 객체로 반환 → 실패 횟수·잠금·접속 로그가 롤백되지 않고 커밋

1. 판정 순서사용자 없음 → 더미 bcrypt 비교(응답 시간 균일화) → 400
1. 로컬 로그인 정보 없음 → 400
1. 이미 잠김 → 423
1. 비밀번호 불일치 → 실패 횟수 +1, 5회째 잠금 → 400 / 423
1. 비활성 계정 → 403 (비밀번호가 맞을 때만 알림)
1. 성공 → 실패 횟수 0, 마지막 로그인 시각 기록

- 모든 시도는 `login_history`에 기록 (IP·사유)
- Redis 세션 생성·JWT 발급은 DB 커밋 이후 (외부 저장소 호출을 트랜잭션 밖에 둠)
- 토큰은 응답 본문이 아닌 `Set-Cookie`(HttpOnly)로만 전달

## 01-2. 인증된 API 요청과 토큰 재발급
- API: 모든 인증 필요 API, `POST /api/auth/refresh`
- 참여자: FE, JwtAuthenticationFilter, AuthSessionLoginResolver, Redis, API Controller, AuthController·Service

### 핵심 포인트
1. 요청마다 필터가 순서대로 판단AccessToken 쿠키가 없으면 필터는 401을 쓰지 않고 필터 체인을 넘긴다. 인증이 필요한 API는 EntryPoint가 오류 속성이 없을 때 401 `UNAUTHORIZED`를 쓴다. 공개 API는 쿠키 없이도 계속된다.
1. JWT parse 실패는 필터가 응답을 바로 쓰지 않고 `ERROR_CODE_ATTRIBUTE`에 401 `EXPIRED_TOKEN` 또는 401 `INVALID_TOKEN`을 남긴 뒤 인증하지 않는다. 인증이 필요한 API의 EntryPoint가 그 코드로 응답한다.
1. Redis 세션이 없으면 `SESSION_EXPIRED` 속성을 남기고 체인을 넘긴다. EntryPoint가 401로 응답한다. Redis `DataAccessException`은 필터가 즉시 503 `EXTERNAL_SERVICE_UNAVAILABLE`을 쓰고 체인을 중단한다.
1. 세션이 있으면 `AuthSessionLoginResolver.resolveAndTouch`가 idle-timeout을 연장한다. 기본값은 `app.auth.idle-timeout=30m`이다.
1. 세션의 mustChangePassword가 참이고 허용 목록 밖이면 필터가 403 `PASSWORD_CHANGE_REQUIRED`를 쓰고 반환한다. 허용되면 SecurityContext에 세션 역할을 넣은 뒤 컨트롤러로 간다. `POST /api/auth/refresh`는 세션 TTL을 연장하지 않는다.
1. 통과 → SecurityContext(역할 = 세션 값) → 컨트롤러

- 필터는 `global`, 세션 확인 구현(`LoginSessionResolver`)만 `authserver` → SSO 전환 시 구현만 교체
- 재발급FE는 `EXPIRED_TOKEN`일 때만 호출, 원 요청당 1회 재시도
- 세션 TTL을 연장하지 않음 → 자동 갱신만으로 30분 무활동 만료가 무력화되지 않음
- 재발급도 401이면 재시도 없이 로그인 화면

## 01-3. 관리자 비밀번호 초기화 → 첫 로그인 → 비밀번호 변경
- API: `POST /api/admin/users/{userId}/password-reset`, `POST /api/auth/login`, `PATCH /api/users/me/password`
- 참여자: 관리자 FE, 사용자 FE, BE(authserver), PostgreSQL, Redis
- 사용 상황: 잠긴 계정 해제(UC-ADM-01 2b), 비밀번호 분실, 신규 사용자 최초 비밀번호 발급

### 핵심 포인트
- 초기화권한 없는 호출은 404 (관리자 API 존재 비노출)
- 트랜잭션 안에서 로그인 정보 교체(잠금 해제·변경 강제) 또는 신규 발급
- 커밋 후 대상 사용자의 모든 세션 삭제
- 임시 비밀번호는 응답에서 1회만 제공 (`Cache-Control: no-store`, 서버는 해시만 저장)

- 첫 로그인응답 `mustChangePassword: true` → FE는 변경 화면 이동
- 변경 전 다른 API는 403 `PASSWORD_CHANGE_REQUIRED`

- 비밀번호 변경현재 비밀번호 확인, 규칙 검사, 기존과 다른지 확인
- 커밋 후 사용자의 모든 세션 삭제(현재 기기 포함), 쿠키 만료 → 새 비밀번호로 재로그인

## 변경 이력
| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| November 8, 2026 | 최초 작성 (흐름 3개). 2026-11-27 develop 50f96d2076b09500c986c684d0468f430a00601e 재대조: 01-1 잠금·판정 순서와 01-3 비밀번호 변경·초기화(커밋 후 세션 삭제, 관리자 API 권한 없음은 404 NOT_FOUND)는 유지. 01-2는 쿠키 없음·토큰 오류·Redis 장애가 필터에서 바로 401을 쓰지 않는 현재 순서로 고침. | SCRUM-96, 커밋 e4c04e0 |
