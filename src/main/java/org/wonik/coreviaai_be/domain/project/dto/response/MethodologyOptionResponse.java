package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;

/**
 * ② 운영 방식 선택지. enabled가 false면 FE는 회색 블럭으로 표시하고 선택을 막는다.
 */
public record MethodologyOptionResponse(Methodology value, boolean enabled) {

	public static MethodologyOptionResponse from(final Methodology methodology) {
		return new MethodologyOptionResponse(methodology, methodology.isEnabled());
	}
}
