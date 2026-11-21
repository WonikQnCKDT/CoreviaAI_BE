package org.wonik.aiserver.domain.wbs.api;

/**
 * 이미 등록된 작업(existingTasks). AI가 같은 작업을 다시 제안하지 않게 하는 참고 정보다.
 * 업무 모듈은 작업의 단계를 저장하지 않아(04. WBS·일정 ERD) stageCode는 항상 null이다.
 */
public record AiWbsExistingTask(Long taskId, String name, String stageCode, String status) {
}
