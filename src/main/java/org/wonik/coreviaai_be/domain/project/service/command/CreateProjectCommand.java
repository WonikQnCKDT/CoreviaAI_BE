package org.wonik.coreviaai_be.domain.project.service.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;

/**
 * 프로젝트 생성 명령(FR-PRJ-01). code는 요청 경계에서 대문자 정규화를 끝낸 값이다.
 *
 * @param startDate          시작일(선택, 생성 후 입력 가능)
 * @param endDate            종료일(선택, 생성 후 입력 가능)
 * @param departmentIds      주관 부서(0~N개). 비어 있을 수 있다
 * @param features           사용할 기능 모듈(FR-PRJ-06). 비어 있을 수 있다
 * @param members            초기 멤버(선택). 비어 있을 수 있고 PM(생성자)은 포함하지 않는다
 * @param gateApproverUserId 게이트 승인자(선택). members 중 한 명이어야 하며 null이면 미지정
 */
public record CreateProjectCommand(
	String name,
	String code,
	LocalDate startDate,
	LocalDate endDate,
	List<Long> departmentIds,
	ProjectVisibility visibility,
	ProjectTemplate template,
	Methodology methodology,
	List<String> phases,
	DepartmentType departmentType,
	Set<ProjectActor> actors,
	Set<ProjectFeature> features,
	List<InitialMember> members,
	Long gateApproverUserId
) {
}
