package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.service.result.NamedRef;

/**
 * ID·이름 쌍(PM, 주관 부서). 없는 사용자·부서면 name이 null이다.
 */
public record NamedRefResponse(Long id, String name) {

	public static List<NamedRefResponse> listOf(final List<NamedRef> refs) {
		return refs.stream().map(ref -> new NamedRefResponse(ref.id(), ref.name())).toList();
	}
}
