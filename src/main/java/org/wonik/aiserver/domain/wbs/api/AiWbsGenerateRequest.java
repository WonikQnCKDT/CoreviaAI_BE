package org.wonik.aiserver.domain.wbs.api;

import java.util.List;

/**
 * WBS 초안 생성 요청(02. AI 생성 API의 WbsGenerateRequest). 필드 이름은 AI API 명세 그대로다.
 * 업무 모듈이 권한(PM)을 확인한 뒤, PM이 볼 수 있는 데이터만 넣는다.
 */
public record AiWbsGenerateRequest(
	Long projectId,
	String projectName,
	String projectPurpose,
	String requirementsText,
	AiWbsProjectContext projectContext,
	List<AiWbsStage> existingStages,
	List<AiWbsExistingTask> existingTasks,
	List<AiWbsTeamMember> teamMembers,
	String userInstruction
) {
}
