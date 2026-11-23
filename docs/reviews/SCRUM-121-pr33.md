# SCRUM-121 / PR #33 검수 및 인수 조건 평가

기준일: 2026-11-23. 기준 develop: `814a489`. PR 원본: `485f576`.

- Jira: https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-121
- 상위 에픽 SCRUM-85, 담당 최준영, 진행 중, Sprint 3 - 일정·업무일지·회의록(id 4).
- 최종 요구사항 3안: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909 (FR-PSN-03·FR-TSK-04, 권한 매트릭스 3.3, 6장 6번).
- API: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/23396356
- ERD: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/23330821
- 시퀀스: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/21496189

## 수정과 검수

1. GitHub PR #33 base를 feature/SCRUM-120-personal-events-api에서 **develop**으로 실제 변경했다. GitHub connector 쓰기 권한 403으로 브라우저에서 변경하고 결과를 확인했다.
2. 기존 공개 이력을 유지하는 merge로 최신 develop 및 PR #32 수정(`8de3fe1`)을 통합했다. 새 worklogs 마이그레이션은 기존 develop의 V16과 충돌하므로 V18로 옮겼다. #32를 먼저 merge하면 #33의 일정 관련 중복 diff는 develop에 흡수된다. 재작성이나 force push는 필요 없다.
3. 직접 `EntityManager.find(..., PESSIMISTIC_WRITE)`에서 발생하는 JPA `PessimisticLockException`·`LockTimeoutException`은 Spring Data 저장소 예외 변환을 거치지 않아 기존 Spring `ConcurrencyFailureException` 전용 처리기에 잡히지 않는다. `CustomException(CONCURRENT_MODIFICATION, cause)`로 변환해 409를 반환하고 원인은 내부에만 보존한다. 잠금 실패 후 저장은 수행하지 않는다.
4. security-first·code-reviewer·java-best-practice-checker를 Java 17 기준으로 적용했다. 입력·소유권·트랜잭션·잠금 실패·동시 첫 저장·원문 비노출을 확인했다. 승인된 본문 상한을 임시 값이라고 잘못 적은 주석을 정정했다. 새 의존성은 없다.

## 보안 브리프

업무일지 원문은 민감한 개인 기록이다. 인증 사용자 ID를 소유자로 고정하고, 본인 날짜 범위 밖 사용자의 원문은 반환하지 않는다. 이번 수정에서 타인 열람 권한은 새로 열지 않는다. PM 조회는 연결 업무의 실제 프로젝트 권한을 검사하는 기능과 함께 구현되어야 하며, PM이라는 이유로 전체 노트를 공개해서는 안 된다. 첫 저장의 사용자 행 잠금과 날짜 유일 제약은 유지하며 실패 시 409로 롤백한다. 잠금 예외의 DB 세부 내용이나 노트 본문을 응답·일반 오류 로그로 보내지 않는다.

## 인수 조건

| 조건 | 판정 | 증거·남은 일 |
|---|---|---|
| 날짜별 Markdown 노트 저장·조회 | 구현 | 본인 GET·PUT, 기간·본문 상한, 중복 날짜 유일 제약 |
| 날짜 선택 시 노트와 연결 업무 표시 | 부분 충족 | 노트 조회만 구현. worklog_tasks 및 연결 업무 응답 없음 |
| 업무별 진척률·소요 시간·완료 입력 | 미충족 | 저장 요청은 content만 받으며 업무 갱신을 호출하지 않음 |
| 업무 및 상위 진척률 자동 갱신(FR-TSK-04) | 미충족 | 최신 develop에 SCRUM-119 진척률 API는 있으나 업무일지와의 연동 없음 |
| 본인·연결 프로젝트 PM만 원문 열람 | 부분 충족 | 본인 격리는 구현. 연결 프로젝트 PM 열람 기능 자체가 없음 |
| 노트↔업무↔캘린더 연동 | 미충족 | 연결 정보 없음. FE 날짜 선택·이동도 별도 검증 필요 |

판정: **부분 구현, FR-PSN-03 전체 인수 불가**. PR의 축소 범위나 삭제 정책 보류는 Jira·최종 명세의 다른 인수 조건을 면제하지 않는다. 연결 업무를 통한 PM 조회 범위와 원문 공개 단위는 요구사항 6장 개인정보 기준으로 구현·검증해야 한다.

## 검증 및 문서

잠금 timeout/획득 실패를 409로 바꾸고 저장을 하지 않는 단위 테스트, 없는 소유자 거부 테스트, 두 동시 첫 요청에서 행 하나·동일 수정 시각을 유지하는 API 통합 테스트를 추가했다.

- JDK 17 `javac --release 17 -parameters`로 main·unit·integration 전체 컴파일 성공. Gradle이 해석한 실제 source set classpath와 Lombok processor를 사용했다.
- JUnit Platform으로 전체 단위·통합 **758건 통과, 실패·생략 0건**(200초). Mockito는 캐시된 Byte Buddy agent를 JVM 시작 시 적용했다. #32 변경과 최신 develop까지 포함한 회귀 결과다.
- Gradle bootJar 패키징 성공. compileJava Worker 제한으로 위 javac 검증 산출물을 입력했으므로 표준 `clean check bootJar` 통과나 JaCoCo 측정으로 간주하지 않는다.
- `git diff --check` 통과. 원격 CI는 push 후 재확인이 필요하다. 배포 서버 SCP 접속 timeout 복구는 확인하지 않았다.
- 로컬 증거: 프로젝트 루트 `.codex-work/scrum121-pr33-junit.log`, `scrum121-pr33-package.log`, `verify-pr.ps1`, `export-pr-classpaths.gradle`.

API의 오류 계약·실제 인수 상태와 ERD의 V17·V18 번호를 갱신했다. 테이블·컬럼·관계는 바뀌지 않아 ERD 그림 변경은 해당 없음이다. 잠금 순서·커밋·롤백이 중요해 [업무일지 저장 시퀀스](../sequence/06-4-worklog-upsert.md) 및 Confluence 시퀀스 본문을 남겼다. 새 PNG 첨부는 추가하지 않았다. 원격 push와 Jira 상태 변경은 하지 않는다.
