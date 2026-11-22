package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

/**
 * 관리자 코드 항목. 사용하지 않는(active=false) 코드도 포함한다.
 */
public record AdminCodeResponse(
	Long id,
	String groupCode,
	String code,
	String labelKey,
	int sortOrder,
	boolean active,
	Instant createdAt,
	Instant updatedAt
) {

	public static AdminCodeResponse from(final String groupCode, final CommonCode code) {
		return new AdminCodeResponse(
			code.getId(),
			groupCode,
			code.getCode(),
			code.getLabelKey(),
			code.getSortOrder(),
			code.isActive(),
			code.getCreatedAt(),
			code.getUpdatedAt()
		);
	}
}
