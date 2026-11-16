package org.wonik.coreviaai_be.domain.project.dto.request;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 멤버 초대(FR-PRJ-04). 생성 화면 ④와 같은 항목(userId, actor)을 쓴다. 한 번에 최대 100명(생성 시 초대와 같은 한도).
 */
public record InviteMembersRequest(
	@NotEmpty(message = "초대할 멤버를 선택해 주세요")
	@Size(max = CreateProjectRequest.MAX_INITIAL_MEMBERS, message = "한 번에 초대할 수 있는 멤버는 최대 100명입니다")
	List<@NotNull(message = "멤버 값을 확인해 주세요") @Valid InitialMemberRequest> members
) {

	public List<InitialMember> toCommand() {
		return members.stream().map(InitialMemberRequest::toCommand).toList();
	}
}
