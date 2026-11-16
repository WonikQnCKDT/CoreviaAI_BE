package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.service.result.ProjectPmResult;

/**
 * PM 위임·재지정 응답. 바뀐 PM만 담는다(재지정은 시스템 관리자가 호출해 프로젝트 정보를 최소로 돌려준다).
 */
public record ProjectPmResponse(Long projectId, String projectCode, Long pmUserId, String pmName) {

	public static ProjectPmResponse from(final ProjectPmResult result) {
		return new ProjectPmResponse(
			result.project().getId(),
			result.project().getCode(),
			result.project().getPmUserId(),
			result.pmName()
		);
	}
}
