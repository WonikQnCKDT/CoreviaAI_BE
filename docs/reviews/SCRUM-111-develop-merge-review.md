# SCRUM-111 — develop 병합 검수

> 검토일: 2026-11-22 · 기준: FR-PSN-01, NFR-07 · Java 17

## 병합 범위

- 사용자가 시작한 `feature/SCRUM-111-todo`의 merge를 이어서 해결했다.
- 기존 HEAD: `f8dc0c3db4226e5b7cd93ac35621bfe200fc7645`.
- 병합 대상 origin/develop: `14b132223ccbd7c7c834299368393c142abd94f9`.
- merge 중단·reset·rebase·다른 브랜치 전환 없이 두 감사 로그 enum 충돌만 해결했다.
- `AuditAction`은 개인 TODO 삭제와 develop의 코드·다국어·WBS·마일스톤·칸반 행위를 모두 보존한다. 총32개이며 양쪽 부모의 상수 합집합과 일치하고 중복이 없다.
- `AuditTargetType`도 PERSONAL_TODO와 develop의 대상 종류를 모두 보존한다. 총9개이며 합집합 일치·중복 없음이다.
- 감사 로그는 EnumType.STRING으로 저장하므로 기존 이름을 변경하거나 삭제하지 않았다. 신규 의존성·API·schema·migration 변경은 없다.

## 검수

- Jira SCRUM-111(상위 SCRUM-85, 진행 중, 담당자 최준영)과 전사관리 명세의 FR-PSN-01·NFR-07을 다시 확인했다.
- Agensi code-reviewer·java-best-practice-checker는 인증 수락 후 재시도 안내가 반복되어 스킬 내용을 받지 못했다. 해당 스킬 실행 성공으로 표현하지 않는다. 가이드의 AI 코드 리뷰 절차와 별도 읽기 검수로 대체했다.
- 명시적 기존 enum 값의 합집합을 선택한 이유, 기존 의존성, 중복·누락 경계, 이름 기반 DB 저장 가정, 컴파일·회귀 검증을 확인했다. 새 보안 설계가 필요한 권한·입력·비밀값 처리 변경은 없다.
- 자동 병합된 README는 develop 기능 요약과 TODO 절을 모두 보존한다. ErrorCode에는 TODO 오류4종이 추가되고, DatabaseCleaner에는 기존 초기화 순서를 유지한 채 personal_todos 삭제가 추가되었음을 확인했다.
- 충돌 표식과 미해결 경로가 없고 직접 수정한 Java 파일의 diff 공백 검사를 통과했다. 전체 병합 diff의 draw.io2개 파일 끝 빈 줄 경고는 들어오는 develop 내용과 같아 보존했다.
- 진행 중 merge를 해결하는 작업으로 새 기능 테스트를 중복 작성하지 않고 기존 전체 단위·통합 테스트를 실행한다.

## 검증 결과

전체 단위473건·통합239건(실패·오류·건너뜀 모두0)과 bootJar가 통과했다. Gradle 종료 코드는0이며 BUILD SUCCESSFUL(4분56초)이다. 로그는 워크스페이스 `.codex-work/scrum111-develop-merge-test.log`, XML 결과는 `build/test-results/test` 및 `build/test-results/integrationTest`에 있다. 마지막 fix 브랜치의475·254건과 다른, 이111 병합 결과의 수치다.

실행 명령:

```powershell
.\gradlew.bat --offline --no-daemon -g <workspace-gradle-home> -I <tests-without-jacoco.gradle> test integrationTest bootJar
```

독립 읽기 검수에서도 두 enum의 합집합·중복 없음, ErrorCode 및 DatabaseCleaner 자동 병합 보존에 문제가 없음을 확인했다.

검증은 저장소 Gradle Wrapper(`.\gradlew.bat`)를 사용한다. 시스템 Gradle 설치는 필요하지 않다. JDK17 및 워크스페이스 오프라인 캐시를 사용하며 기존 환경의 JaCoCo0.8.15 에이전트 부재 때문에 워크스페이스 init script로 테스트 수집만 비활성화한다. 저장소 build.gradle·CI는 바꾸지 않는다. 커버리지·전체check 통과나 실제 PostgreSQL 부하 검증을 주장하지 않는다.

## 문서·후속

- API·schema 계약 및 트랜잭션 흐름을 새로 바꾸지 않아 Confluence API·ERD·시퀀스 수정은 해당 없음이다. 기존 기능 계약과 이번 merge 검증을 구분해 이 문서에 기록한다.
- `fix/sprint2-final-verification`의 TODO 피드 필터·JPA 잠금409·멤버 제외 차단·앱 로그 마스킹 보완은 이번 merge에 가져오지 않았다. 해당 브랜치를 마지막 별도 PR로 반영하는 기존 계획을 유지한다.
- 원격 push와 Jira 상태 변경은 하지 않는다. 사용자가 보관한 추적되지 않는 파일은 수정하지 않는다.
