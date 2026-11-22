package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;

public record AdminCodeGroupResponse(
	Long id,
	String groupCode,
	String labelKey,
	Instant createdAt,
	Instant updatedAt
) {

	public static AdminCodeGroupResponse from(final CodeGroup group) {
		return new AdminCodeGroupResponse(
			group.getId(),
			group.getGroupCode(),
			group.getLabelKey(),
			group.getCreatedAt(),
			group.getUpdatedAt()
		);
	}
}
