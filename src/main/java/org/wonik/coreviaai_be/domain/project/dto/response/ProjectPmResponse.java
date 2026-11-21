package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.service.result.ProjectPmResult;

/**
 * 관리자 PM 재지정 응답. 시스템 관리자는 프로젝트 데이터를 볼 수 없어 식별 정보와 PM 목록만 담는다.
 */
public record ProjectPmResponse(Long projectId, String projectCode, List<NamedRefResponse> pms) {

	public static ProjectPmResponse from(final ProjectPmResult result) {
		return new ProjectPmResponse(
			result.project().getId(),
			result.project().getCode(),
			NamedRefResponse.listOf(result.pms())
		);
	}
}
