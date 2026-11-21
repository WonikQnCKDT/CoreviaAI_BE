package org.wonik.coreviaai_be.domain.project.service.result;

/**
 * 화면 표시용 ID·이름 쌍(PM, 주관 부서 등). 없는 사용자·부서면 name이 null이다.
 */
public record NamedRef(Long id, String name) {
}
