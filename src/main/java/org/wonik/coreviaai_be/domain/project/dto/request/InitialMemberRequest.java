package org.wonik.coreviaai_be.domain.project.dto.request;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 초대하는 멤버 한 명(생성 화면 ④, FR-PRJ-04 멤버 초대 공용). actor는 지정한 액터 중 하나여야 한다(서비스에서 검증).
 */
public record InitialMemberRequest(
	@NotNull(message = "초대할 사용자를 선택해 주세요")
	@Positive(message = "사용자 ID를 확인해 주세요")
	Long userId,

	@NotNull(message = "멤버 역할을 선택해 주세요")
	ProjectActor actor
) {

	public InitialMember toCommand() {
		return new InitialMember(userId, actor);
	}
}
