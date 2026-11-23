package org.wonik.aiserver.domain.wbs.api;

/**
 * 이미 등록된 작업(existingTasks). AI가 같은 작업을 다시 제안하지 않게 하는 참고 정보다.
 * phaseId는 작업에 연결된 프로젝트 단계이며 연결이 없으면 null이다.
 */
public record AiWbsExistingTask(Long taskId, String name, Long phaseId, String status) {
}
