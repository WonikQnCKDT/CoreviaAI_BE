# SCRUM-111 소프트 삭제 보안 설계·검수 기록

> 상태: Draft · 담당자: 미정 · 마지막 검토일: 2026-11-22
> 기준: FR-PSN-01·3.3 본인 CRUD, 사용자 후속 결정(소프트 삭제 승인)
> 작업 기준: 통합 초안 b5ee98a. 원격 develop 기준은 원작업 6d39260을 유지한다.

## 1. 확인한 결정과 범위

- Jira SCRUM-111(진행 중·최준영·상위 SCRUM-85), 요구사항 최종안8126909(v18), 개인 업무 ERD20414469(v4)를 다시 읽었다. 프로젝트 AGENTS/PROJECT·공통·Backend 가이드와 저장소 README/AGENTS·공통 코드/다국어/테마 지침을 다시 확인했다.
- 승인 요청 원천은 사용자 승인에 따라 후속 연결한다. 개인 TODO는 소프트 삭제로 확정했다. 삭제 정책 확정은 이 도메인에만 적용하며 다른 개인 일정·노트 등 정책을 임의로 확정하지 않는다.
- 마감 임박 기본값은 사용자가 최종 3일 적용을 승인했다. dueSoonDays 생략 시3, 1~365 명시 override를 허용한다. 오늘 제외 D+1~D+3이 기본 임박 범위다.

## 2. 구현 전 보안 브리프

| 항목 | 설계·검증 계획 |
| --- | --- |
| 신뢰 경계 | DELETE의 양수 path ID·기존 인증 principal → AuditActor → 본인 ID와 항목 ID를 함께 제한한 행 잠금 → DB 변경·공통 AuditLogger. body로 소유자 받지 않음 |
| 삭제 의미 | nullable deleted_at UTC 시각만 표시하고 개인 데이터·불변 생성 키/해시는 보존. 자동 물리 purge·복구 API·보존 기간은 미정이므로 도입하지 않음 |
| 인가·존재 비노출 | 타인·없는 항목은 같은404. 본인 삭제행에 반복 DELETE는204. 삭제행 GET/PATCH는404. SYSTEM_ADMIN도 자신의 항목만 삭제 가능 |
| 조회 경계 | 단건 조회·변경과 모든 개인 목록(done false/true·cursor continuation)에 deleted_at IS NULL. 프로젝트 WBS는 소프트 삭제 대상 아님 |
| 멱등성·부활 방지 | 삭제행도 user_id/request_id unique 유지. POST 같은 키·같은 최초 내용은409 TODO_DELETED, 다른 내용은 기존409 TODO_REQUEST_CONFLICT. 새 행 생성·복구 금지 |
| 동시성·감사 | 본인 행 잠금으로 DELETE를 순차화. 최초 요청만 deleted_at 설정 및 PERSONAL_TODO_DELETED 감사1건. 반복 요청은 감사 추가 없음. 삭제와 감사는 같은 트랜잭션으로 감사 실패 시 삭제도 롤백 |
| 감사 최소 정보 | 공통 AuditActor의 행위자·IP, target ID·종류, 필요한 ownerUserId만. 제목·본문·requestId·requestHash 미기록 |
| 시각 | 주입 Clock UTC, 저장 후 refresh로 TIMESTAMP(6) 정밀도 반영. 삭제 재전송은 최초 시각 유지 |
| 마이그레이션 | 적용됐을 수 있는 V13 수정 금지. V14로 nullable 컬럼만 추가(expand), 기존행 null. V8~V12 → V13 → V14 순서의 기존 배포 전제 유지 |
| 검증 | 본인·타인·미인증, 재삭제204·감사1건, 삭제후GET/PATCH404, done 양쪽/기존cursor 제외, POST 부활 차단, 동시DELETE 감사1건, 감사 실패롤백, DATE/나노초 경계 기존 테스트 유지 |

root가 이번 작업에도 Agensi security-first를 다시 호출했으나 UNAUTHORIZED 재인증 오류로 읽지 못했다. 프로젝트 공통 보안 지침과 AI 코드 리뷰 절차로 보안 브리프를 작성한다. code-reviewer·java-best-practice-checker도 최신 재호출 결과를 확인하고 불가하면 동일 절차로 검수한다. 스킬 파일을 저장하지 않는다.

후속 재호출: code-reviewer·java checker에서 재인증 요청이 승인되었다며 재시도를 요구했으나, security-first·code-reviewer·java-best-practice-checker 모두 다시 호출한 최종 결과도 재인증 필요(UNAUTHORIZED)였다. 실제 스킬 내용은 반환되지 않았다. 프로젝트 AI 코드 리뷰 순서·독립 읽기 검수로 대체한다.

## 3. 검수·테스트 결과

구현 후 실제 결과를 기록한다. 표준 check/커버리지는 기존 JaCoCo0.8.15 오프라인 캐시 부재로 검증되지 않으며, workspace 전용 대체 init을 사용한 테스트·빌드와 구분한다. formatter/lint/static-analysis·dependency lock·security-scan 미구축의 기존 한계와 승인 주체 미정은 이전 검수 기록을 계속 적용한다.

- 최초 전체 실행: 단위303 성공, 통합146 중145 성공·감사 실패 주입 테스트1개 실패. Mockito의 doThrow 스텁 설정이 MANDATORY 전파의 AuditLogger AOP proxy를 통과하면서 테스트 설정 단계에 트랜잭션 없음 오류가 났다.
- 자동 수정: AopTestUtils로 실제 target spy를 얻어 실패를 주입하도록 **테스트만** 수정했다. 프로덕션 삭제·감사 코드를 완화하지 않았다. 해당 테스트는 실제 삭제 중 감사 예외가 나면 삭제 시각·감사 모두 롤백되고, 정상 재시도가 성공하는지 확인한다.
- 생성·삭제·수정·개인/업무 분리의 권한 경계, 고정 UUID 멱등키 보존, UTC 시각·DB 정밀도, 실패 롤백을 테스트·독립 리뷰로 검수한다. 새 dependency·비밀값·외부 호출은 없다.

### 최종 검증 (2026-11-22 10:33:23 KST)

```powershell
.\gradlew.bat --offline --no-daemon -g ../.gradle-home -I ../tests-without-jacoco.gradle test integrationTest bootJar
```

- **최종 소스 단위303·전체 통합146, 실패·오류0, bootJar 성공**. 최종 전체 실행은2분58초. 단위·bootJar는 동일한 최종 프로덕션/단위 소스에서 직전 실행한 결과를 Gradle UP-TO-DATE로 재사용했고, 수정된 fault 주입 테스트를 포함한 통합146개는 전부 새로 실행했다.
- fault 주입 수정 후 focused7개도 먼저 통과했다(10:30:18 KST). 최종 전체146개에 포함되므로 숫자를 더해 중복 집계하지 않는다. TODO 통합은 기존15+소프트 삭제/기본값7개다.
- `git diff --check` 성공. V13은 b5ee98a 대비 변경 없음. V14만 추가. 보완 drawio XML 파싱 성공. 신규 라이브러리·빌드/CI 설정 변경 없음.
- 독립 읽기 검수: 본인 범위·삭제행 조회 차단·키 보존·동시/반복 삭제 감사1건·MANDATORY 프록시 유지와 fault 주입 방식·V13 불변·최신 문서 확인, 미수정 확정 지적 없음.

작업 공간 전용 `../tests-without-jacoco.gradle`은 저장소에 commit하지 않는다. 내용은 다음과 같다.

```groovy
gradle.projectsEvaluated {
    allprojects { project ->
        project.tasks.withType(Test).each { testTask ->
            testTask.extensions.getByType(org.gradle.testing.jacoco.plugins.JacocoTaskExtension).enabled = false
        }
    }
}
```

위 결과는 JaCoCo instrumentation을 끈 동작·빌드 증거다. 표준 check·커버리지나 기존 미구축 formatter/lint/static/dependency-lock/security-scan까지 통과했다는 뜻이 아니다. 승인 주체 미정·영향·대안·merge 전 재검토 조건은 이전 검수 기록6절에 남아 있다.

## 4. MUST 확인과 인계

- 본인 principal 기반 자원 인가·양수 path 검증·타인 존재 비노출, parameter binding, UTC Clock·DB 정밀도, 삭제와 감사 동일 트랜잭션, 멱등 DELETE·생성키 부활 차단을 확인했다.
- 개인 목록(done false/true·기존cursor)은 삭제행을 제외하고, WBS 업무는 삭제하지 않는 것을 검증했다. FE 화면 코드가 없는 BE 변경이므로 화면 하드코딩0건·1분 재조회 UI 검수는 해당 FE 작업에서 확인해야 한다.
- 적용됐을 수 있는 migration 수정 금지(V13 불변), V14 expand·배포 순서·구 리더 노출 위험·roll-forward를 문서화했다. 운영 전 실제 PostgreSQL 실행 계획·잠금 동작·배포 검증은 별도 필요하다.
- API/ERD/보완 drawio/생성·삭제 시퀀스/README는 사용자3결정을 반영했다. 기존 검수 기록은 역사적 결과로 보존했다. 원격 Confluence/Jira 게시·댓글은 root가 수행하며 이 에이전트는 원격 push·상태 변경을 하지 않았다.
- 승인 원천 연결은 사용자 승인 후속 범위다. 삭제행·키 보존 기간·물리 purge·복구 정책은 운영 미정이므로 자동 작업을 추가하지 않았다. ERD 기존 원본 다운로드·PNG export는 도구 환경에서 불가해 기존 원격 그림을 보존하고 로컬 보완 drawio를 유지한다.
