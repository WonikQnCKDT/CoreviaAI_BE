package org.wonik.coreviaai_be.domain.admin.service.command;

/**
 * 이름·부서·직무 수정 입력. 보낸 값으로 덮어쓰므로 departmentId·jobTitle이 null이면 비운다.
 */
public record UpdateUserProfileCommand(String name, Long departmentId, String jobTitle) {
}
