package org.wonik.coreviaai_be.domain.project.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.command.CreateProjectCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 생성 요청(FR-PRJ-01). 단계형 화면 ① 기본 정보 → ② 템플릿·운영 방식 → ③ 액터 → ④ 기능 구성 →
 * ⑤ 멤버 초대(선택) → ⑥ 게이트 승인자(선택)의 입력을 FE가 모아 마지막에 한 번 보낸다.
 * PM은 받지 않는다(생성자 자동 지정). 기간·주관 부서는 생성 후 입력할 수 있어 선택이다.
 * 코드는 영문·숫자·하이픈 2~20자로 받고 대문자로 바꿔 저장해, 대소문자만 다른 코드를 중복으로 본다.
 */
public record CreateProjectRequest(
	@NotBlank(message = "프로젝트 이름을 입력해 주세요")
	@Size(max = 100, message = "프로젝트 이름은 100자 이하로 입력해 주세요")
	String name,

	@NotBlank(message = "프로젝트 코드를 입력해 주세요")
	@Pattern(regexp = CODE_PATTERN, message = "프로젝트 코드는 영문·숫자로 시작하는 영문·숫자·하이픈(-) 2~20자로 입력해 주세요")
	String code,

	LocalDate startDate,

	LocalDate endDate,

	@Size(max = MAX_DEPARTMENTS, message = "주관 부서는 최대 20개까지 지정할 수 있습니다")
	List<@NotNull(message = "부서 ID를 확인해 주세요") @Positive(message = "부서 ID를 확인해 주세요") Long> departmentIds,

	@NotNull(message = "공개 범위를 선택해 주세요")
	ProjectVisibility visibility,

	@NotNull(message = "템플릿을 선택해 주세요")
	ProjectTemplate template,

	@NotNull(message = "운영 방식을 선택해 주세요")
	Methodology methodology,

	@Size(max = MAX_PHASES, message = "프로젝트 단계는 최대 20개까지 지정할 수 있습니다")
	List<@NotBlank(message = "단계 이름을 입력해 주세요") @Size(max = MAX_PHASE_NAME_LENGTH,
		message = "단계 이름은 100자 이하로 입력해 주세요") String> phases,

	@NotNull(message = "부서 유형을 선택해 주세요")
	DepartmentType departmentType,

	@NotEmpty(message = "액터를 지정해 주세요")
	Set<@NotNull(message = "액터 값을 확인해 주세요") ProjectActor> actors,

	@NotNull(message = "기능 구성을 선택해 주세요")
	Set<@NotNull(message = "기능 모듈 값을 확인해 주세요") ProjectFeature> features,

	@Size(max = MAX_INITIAL_MEMBERS, message = "생성 시 초대할 수 있는 멤버는 최대 100명입니다")
	List<@NotNull(message = "멤버 값을 확인해 주세요") @Valid InitialMemberRequest> members,

	@Positive(message = "게이트 승인자 ID를 확인해 주세요")
	Long gateApproverUserId
) {

	public static final String CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9-]{1,19}$";
	public static final int MAX_INITIAL_MEMBERS = 100;
	public static final int MAX_DEPARTMENTS = 20;
	public static final int MAX_PHASES = 20;
	public static final int MAX_PHASE_NAME_LENGTH = 100;

	/** departmentIds·members를 생략하면 빈 목록으로 본다(주관 부서 없음, PM만 있는 프로젝트). */
	public CreateProjectCommand toCommand() {
		return new CreateProjectCommand(
			name,
			normalizeCode(code),
			startDate,
			endDate,
			departmentIds == null ? List.of() : List.copyOf(departmentIds),
			visibility,
			template,
			methodology,
			phases == null ? template.getDefaultPhases() : phases.stream().map(String::strip).toList(),
			departmentType,
			Set.copyOf(actors),
			Set.copyOf(features),
			members == null ? List.of() : members.stream().map(InitialMemberRequest::toCommand).toList(),
			gateApproverUserId
		);
	}

	public static String normalizeCode(final String code) {
		return code.toUpperCase(Locale.ROOT);
	}
}
