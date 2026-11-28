# 04. WBS·일정 — 마일스톤 (API)

> 상태: Active · 마지막 검토일: 2026-11-28
> 기준: FR-WBS-04, 요구사항 명세서 3.3 WBS-04·3.2 공통 규칙, ERD 04 (`milestones`, `milestone_tasks`)

- 관련: [SCRUM-108](https://jnu-team-aaptc1y6.atlassian.net/browse/SCRUM-108), [전사관리 요구사항](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/8126909), [04. WBS·일정 (API)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/18907137).
- 공통 응답·인증·오류 형식은 Confluence [00. 공통 규약 (API)](https://jnu-team-aaptc1y6.atlassian.net/wiki/spaces/CoreviaAI/pages/12877846)을 따른다.

## 요약

| Method | Path | 설명 | 권한 | 상태 |
| --- | --- | --- | --- | --- |
| GET | `/api/projects/{projectId}/wbs/milestones` | 마일스톤 전체 목록(목표일·ID 순, 최대 200건) | 프로젝트 열람 가능자 전원 | 구현 |
| GET | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | 마일스톤 상세 | 프로젝트 열람 가능자 전원 | 구현 |
| POST | `/api/projects/{projectId}/wbs/milestones` | 마일스톤 등록(`MANUAL`) | PM | 구현 |
| PATCH | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | 이름·목표일 수정(낙관적 잠금 `version`) | PM | 구현 |
| PUT | `/api/projects/{projectId}/wbs/milestones/{milestoneId}/tasks` | 연결 작업 전체 교체(변경 시 `version + 1`) | PM | 구현 |
| DELETE | `/api/projects/{projectId}/wbs/milestones/{milestoneId}` | 마일스톤 삭제(연결 정보 함께 삭제, 감사 로그) | PM | 구현 |

## 공통 사항

- **권한(3.3 WBS-04)**: 조회는 프로젝트를 볼 수 있는 사용자 전원(멤버·공개 범위 열람자·소속 범위 경영진), 변경(등록·수정·연결 교체·삭제)은 PM만(`403 PROJECT_PM_ONLY`). 볼 수 없는 프로젝트는 `404 PROJECT_NOT_FOUND`, WBS 기능 꺼짐은 `404 FEATURE_DISABLED`, 다른 프로젝트·없는 마일스톤은 `404 MILESTONE_NOT_FOUND`. `SYSTEM_ADMIN`은 `/api/projects/**` 접근 시 `404`.
- **업무 날짜 기준**: 주입된 `Clock`의 `Asia/Seoul`(`BusinessCalendar.ZONE`) 달력 날짜를 '오늘'로 사용한다. `dDay`·`delayed`·`incompleteTaskCount`는 저장하지 않고 매 조회 시 계산한다.
- **D-day 계산**: `dDay = 목표일 - 오늘`(일). 양수는 남은 날(`D-n`), `0`은 당일(`D-day`), 음수는 지난 날(`D+n`).
- **지연 경고(`delayed`)**: `dDay <= 0`(오늘 ≥ 목표일, **기한 당일 포함**)이고 연결 작업 중 완료(`DONE`)가 아닌 작업이 1건 이상(`incompleteTaskCount > 0`)일 때 `true`. 연결 작업이 없으면(`linkedTaskCount == 0`) 판단 근거가 없어 `false`.
- **연결 작업 완료·상태 판정 규칙** (FE `wbsRules.deriveDisplayStatus`와 동일):
  - **하위작업이 없는 작업**(말단 최상위 작업 또는 하위작업): 저장된 `wbs_tasks.status`를 그대로 사용한다.
  - **하위작업이 있는 상위 작업**: 부모의 저장된 `status`가 아니라 **직속 하위작업들의 상태**에서 유효 상태를 파생한다(WBS 2단계 계층).
    - 하위작업 전부 `DONE` → `DONE` (완료 판정, 경고 해제)
    - 하위작업 전부 `TODO` → `TODO`
    - 하위작업 전부 `REVIEW` 또는 `DONE`(전부 `DONE` 제외) → `REVIEW`
    - 그 외 혼합 → `IN_PROGRESS`
  - 응답의 `tasks[].status`, `incompleteTaskCount`, `delayed` 모두 이 유효 상태를 기준으로 계산한다.
  - 조회 효율: 연결 작업 조인 조회 1회 + 최상위 연결 작업의 하위작업 상태 배치 조회 1회로 N+1을 방지한다.
- **상한·연결 제약**: 프로젝트당 마일스톤 최대 200개(`409 MILESTONE_LIMIT_EXCEEDED`), 마일스톤당 연결 작업 최대 200개. 연결 작업은 같은 프로젝트의 WBS 작업·하위작업만 허용하며 없는 ID와 다른 프로젝트 ID를 구분하지 않고 `400 INVALID_MILESTONE_TASK`로 응답한다.

## 응답 형식 (`MilestoneResponse`)

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 마일스톤 ID |
| `name` | string | 이름(1~200자) |
| `targetDate` | date | 목표일(`YYYY-MM-DD`) |
| `source` | string | `MANUAL` \| `AI_DRAFT` |
| `dDay` | number | 오늘(`Asia/Seoul`)부터 목표일까지 일수 |
| `delayed` | boolean | 지연 경고 여부(`dDay <= 0 && incompleteTaskCount > 0`) |
| `linkedTaskCount` | number | 연결 작업 수 |
| `incompleteTaskCount` | number | 유효 상태가 `DONE`이 아닌 연결 작업 수 |
| `tasks` | array | 연결 작업 요약(`id`, `parentTaskId`, `name`, `status`(유효 상태), `endDate`), `id` 오름차순 |
| `version` | number | 낙관적 잠금 버전 |

## 변경 이력

| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| 2026-11-28 | 연결 작업 완료·상태 판정에 하위작업 기준 파생 규칙 반영(FE `deriveDisplayStatus` 정합) | SCRUM-108, FR-WBS-04 |
