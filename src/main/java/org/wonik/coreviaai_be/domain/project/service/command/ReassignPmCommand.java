package org.wonik.coreviaai_be.domain.project.service.command;

/**
 * 시스템 관리자의 PM 재지정(FR-PRJ-04, 요구사항 명세서 3.1 예외). 인사 발령으로 정해진 새 PM을
 * 프로젝트 코드(대문자로 정규화된 값)와 사용자 ID만으로 배치한다.
 */
public record ReassignPmCommand(String projectCode, Long newPmUserId) {
}
