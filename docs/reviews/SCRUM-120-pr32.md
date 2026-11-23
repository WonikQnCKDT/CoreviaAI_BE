# SCRUM-120 / PR #32 검수 및 인수 조건 평가

기준일: 2026-11-23. 기준 develop: `814a489`. PR 원본: `38bff9d`.

- Jira: https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-120
- 상위 에픽 SCRUM-85, 담당 최준영, 진행 중, Sprint 3 - 일정·업무일지·회의록(id 4).
- 최종 요구사항 3안: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909 (FR-PSN-02, 3.3 권한, 6장 개인정보).
- API: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/23396356
- ERD: https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/23330821

## 수정과 검수

1. CI verify 실행 45142179953에서 261개 통합 테스트가 Flyway 초기화에 실패했다. 최신 develop의 V15와 개인 일정 V15가 충돌하므로 미병합 개인 일정 마이그레이션만 V17로 이동했다. 기존 develop 마이그레이션 내용은 변경하지 않았다.
2. 기간 제한은 일정 건수 상한이 아니다. BE 원칙의 목록 페이지네이션 MUST에 따라 offset page/size, 최대 100건, startAt/id 안정 정렬, hasNext를 적용했다. 응답 배열이 페이지 객체로 바뀌므로 소비자는 response.items로 변경해야 한다. 동시 등록·수정 중 페이지 이동은 스냅샷 조회를 보장하지 않는다.
3. CD의 빈 branches 설정에서 feature push도 배포를 실행한 기록이 있다. 배포 대상을 develop push로 명시했다. 기존 CD의 SCP 접속 timeout 자체는 서버·네트워크 운영 점검 대상이며 코드 수정으로 접속 복구를 검증한 것은 아니다.
4. Agensi security-first, code-reviewer, java-best-practice-checker를 적용했다. Java 17 범위에서 소유자 조건, 입력 상한, 실패 응답, 정렬·페이지 경계, SQL 바인딩, 의존성 추가 없음, 원문 로그 노출 여부를 검토했다.

## 보안 브리프

인증된 사용자 ID와 외부 입력(기간·본문·페이지)을 신뢰 경계로 둔다. 개인 일정의 제목·메모는 민감한 사용자 기록이므로 본인 조건을 DB 조회에 고정하고 타인 항목은 404로 숨긴다. 기존 인증·소유권 모델을 유지하며 PM 등 타인 열람 범위를 새로 추가하지 않는다. SQL은 파라미터 바인딩하고 요청 toString은 원문을 가린다. 테스트는 타인 목록·수정 차단, 미인증, 기간 경계와 페이지 경계를 확인한다.

## 인수 조건

| 조건 | 판정 | 증거·남은 일 |
|---|---|---|
| 개인 일정 등록·본인만 조회 | 구현 | 소유자 ID 조건 및 API 통합 테스트. 타인 수정도 404 |
| 담당 업무 기간·마일스톤 변경 즉시 캘린더 반영 | 미충족 | personal_events만 조회. wbs_tasks·milestones 통합 조회 없음 |
| 회의 일정 자동 표시 | 미충족 | 회의 일정 통합 소스 없음 |
| 프로젝트별 색상 구분 | 미충족 | 통합 항목의 프로젝트 식별·색상 계약 없음. FE 표시 검증도 필요 |
| 날짜 클릭 시 업무·노트 표시 및 상호 이동 | 미충족 | 날짜별 업무·노트 연결 응답 없음. FE 월·주·일·클릭 이동 검증 필요 |

판정: **부분 구현, FR-PSN-02 전체 인수 불가**. 삭제 API 보류는 위 인수 조건을 면제하지 않는다.

## 검증 및 배포

페이지 동률 순서, 다음/마지막/빈 페이지, 타인 항목 격리, page=-1 및 size=0/100/101 검증을 추가했다.

- JDK 17 `javac --release 17 -parameters`로 main·unit·integration 전체 소스 컴파일 성공. Lombok processor와 classpath는 Gradle이 해석한 실제 source set 의존성을 사용했다.
- JUnit Platform으로 unit·integration 전체 **751건 통과, 실패·생략 0건**(240초). Mockito는 동적 attach 대신 캐시된 Byte Buddy agent를 JVM 시작 시 적용했다. 기존 Flyway 초기화 실패를 포함해 전체 회귀를 확인했다.
- Gradle bootJar 패키징 성공. 환경 제한으로 compileJava task를 다시 실행하는 대신 위에서 검증한 동일 소스의 javac 산출물을 입력했다.
- `git diff --check` 통과. 표준 `clean check bootJar`는 Gradle 데몬/컴파일 Worker 연결 제한으로 완료하지 못했으며 JaCoCo 커버리지와 원격 CI 통과는 주장하지 않는다. push 후 표준 CI 재확인이 필요하다.
- 로컬 증거: 프로젝트 루트 `.codex-work/scrum120-pr32-junit-agent.log`, `scrum120-pr32-package.log`, `verify-pr.ps1`, `export-pr-classpaths.gradle`.

테이블·컬럼·관계 자체는 바뀌지 않아 ERD 그림 변경은 해당 없음이다. 개인 일정 조회·등록·수정은 외부 호출이나 새로운 잠금 흐름이 없는 CRUD이므로 새 시퀀스를 만들지 않았다. Confluence API·ERD 텍스트는 번호 및 실제 구현 상태를 갱신했다. 원격 push와 Jira 상태 변경은 수행하지 않는다.
