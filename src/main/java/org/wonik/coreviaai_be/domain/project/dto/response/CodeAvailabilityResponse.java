package org.wonik.coreviaai_be.domain.project.dto.response;

/**
 * 프로젝트 코드 중복 확인 결과.
 *
 * @param code      저장될 형태로 정규화한 코드(대문자)
 * @param available 지금 사용할 수 있는지. 생성 시점에 다시 확인하므로 확정 값은 아니다
 */
public record CodeAvailabilityResponse(String code, boolean available) {
}
