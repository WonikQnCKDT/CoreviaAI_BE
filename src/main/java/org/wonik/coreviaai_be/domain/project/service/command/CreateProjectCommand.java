package org.wonik.coreviaai_be.domain.project.service.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;

/**
 * 프로젝트 생성 명령(FR-PRJ-01). code는 요청 경계에서 공백 제거·대문자 정규화를 끝낸 값이다.
 *
 * @param members            초기 멤버(선택). 비어 있을 수 있고 PM(생성자)은 포함하지 않는다
 * @param gateApproverUserId 게이트 승인자(선택). members 중 한 명이어야 하며 null이면 미지정
 */
public record CreateProjectCommand(
	String name,
	String code,
	LocalDate startDate,
	LocalDate endDate,
	Long departmentId,
	ProjectVisibility visibility,
	Methodology methodology,
	DepartmentType departmentType,
	Set<ProjectActor> actors,
	List<InitialMember> members,
	Long gateApproverUserId
) {
}
