<!-- Confluence Sequence 본문과 같은 판정 순서. develop 50f96d2076b09500c986c684d0468f430a00601e. draw.io·PNG는 이 파일에 넣지 않았고, 첨부 완료로 보지 않는다. -->

# 05. 업무·보드 (Sequence)
상태: 현재 구현 대조 · 담당자: 미정 · 마지막 검토일: 2026-11-27. 기준 소스: develop 50f96d2076b09500c986c684d0468f430a00601e (실제 Controller·Service·Policy·Repository·ExceptionHandler 대조).
공통: 기존 JWT·세션 보안 체인과 입력 검증을 먼저 통과한다. 오류는 서비스 표준 ApiResponse이며 본문에 내부 스택·비밀값을 노출하지 않는다. 아래는 API 계약을 새로 바꾸는 문서가 아니라 현재 구현 순서를 정정한 기록이다.
기준: 요구사항명세 최종안: 전사관리 · 3.3 권한 매트릭스. 도식의 화살표는 호출·응답이며 노란 상자는 분기·트랜잭션 조건이다.
## 05-1. 칸반 카드 이동 · COUNT 기반 WIP 경고
API: PUT /api/projects/{projectId}/board/cards/{taskId}/column
참여자: PM·담당자 FE / API, BoardService / Editor, BoardReader / History, PostgreSQL
### 핵심 포인트
BoardReader.cardCount는 countByProjectIdAndBoardColumnId와 상태별 첫 컬럼 조회를 사용한다. 그 컬럼이 같은 상태의 첫 컬럼일 때만 board_column_id IS NULL AND status가 같은 카드 COUNT를 더한다. moveCard에서 보드 전체를 엔티티로 다시 읽는다고 설명하지 않는다. DONE으로 바뀌면 completed_at(최초만)·progress100, DONE에서 벗어나면 completed_atNULL이며 progress는 유지한다. 같은 상태의 다른 컬럼 이동은 상태 변경 시각을 갱신하지 않는다. 조회·컬럼 단순CRUD는 코드 흐름이 명확해 별도 시퀀스를 늘리지 않는다.
그림 원본: docs/sequence/05-1-board-card-move.drawio / 내보낸 PNG: 05-1-board-card-move.png. 두 파일은 실제 생성·가독성 검수 완료. 현재 이 절의 최신 그림 첨부·본문 삽입은 대기 상태이며 첨부 완료로 간주하지 않는다.
### 판정 순서
1. PM·담당자 FE / API → BoardService / Editor: columnId·version 검증 후 moveCard
1. BoardService / Editor → PostgreSQL: BEGIN · 공개 범위404 → KANBAN 기능404 → projects FOR UPDATE
1. BoardService / Editor → PostgreSQL: 업무 조회404 → PM 또는 본인 담당 개발자/QA403 → 컬럼 조회404
1. 같은 컬럼: version 미검사·변경/이력 없음. 다른 컬럼: version 비교409 후 상태 동기화.
1. BoardService / Editor → PostgreSQL: moveToColumn · 상태 변경 시각 / DONE 완료 시각·progress 처리 · FLUSH
1. BoardService / Editor → BoardReader / History: 변경 시 STATUS_CHANGED 또는 BOARD_COLUMN_CHANGED / 진척 이력. 하위작업이고 progress가 바뀌면 같은 트랜잭션에서 부모의 말단 가중 평균을 다시 계산하고 부모 이력도 남긴다
1. BoardReader / History → PostgreSQL: task_histories INSERT (같은 트랜잭션)
1. BoardService / Editor → BoardReader / History: cardCount(target): 보드 전체 엔티티 조회 없이 집계
1. BoardReader / History → PostgreSQL: COUNT 명시 컬럼 카드 + 상태별 첫 컬럼이면 NULL컬럼·같은상태 COUNT
1. BoardReader / History → BoardService / Editor: 카드 수 > WIP 제한이면 warning=true (이동 차단 안 함)
1. PostgreSQL → BoardService / Editor: COMMIT · 상태·진척·이력·완료 처리 함께 확정
1. BoardService / Editor → PM·담당자 FE / API: 200 card·column(cardCount/wipExceeded)·wipWarning
1. 실패는 모두 ROLLBACK. 카드 이동·컬럼 관리·WBS 편집·AI 반영은 같은 프로젝트 행 잠금으로 직렬화.

## 변경 이력
November 22, 2026 — 현재 구현 대조: 복수 PM·복수 부서/생성 권한 개방·폐지 위임·관리자 복구, WBS 상한/토큰/잠금, COUNT WIP, TODO 멱등/소프트 삭제/raw JPA409 중 해당 흐름 정정. 모든 그림은 draw.io와 PNG를 동일 좌표 모델에서 생성. 새 첨부는 진행 상태에 따라 별도 확인한다.
| 날짜 | 내용 | 관련 |
| --- | --- | --- |
| November 22, 2026 | 최초 작성: 05-1 칸반 카드 이동. 2026-11-27 develop 50f96d2076b09500c986c684d0468f430a00601e BoardService.moveCard 재대조: 같은 컬럼은 version을 보지 않고, 다른 컬럼만 409 후 moveToColumn. DONE 진입은 completedAt(최초만)과 progress 100, DONE에서 벗어나면 completedAt만 지운다. WIP는 COUNT 경고이며 이동을 막지 않는다. 하위작업 진척 변화가 있으면 부모 가중 진척률을 같은 트랜잭션에서 갱신한다. 컬럼 CRUD 본문은 이번에도 시퀀스로 늘리지 않았다. | SCRUM-110 |
