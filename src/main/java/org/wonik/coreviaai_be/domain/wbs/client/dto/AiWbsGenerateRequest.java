package org.wonik.coreviaai_be.domain.wbs.client.dto;

import java.util.List;

/**
 * AI 서버 POST /generate/wbs 요청(WbsGenerateRequest, 02. AI 생성 API). 필드 이름은 AI API 명세 그대로다.
 * BE가 권한(PM)을 확인한 뒤, PM이 볼 수 있는 데이터만 넣는다.
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
