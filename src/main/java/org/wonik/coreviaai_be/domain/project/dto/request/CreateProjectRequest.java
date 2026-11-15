package org.wonik.coreviaai_be.domain.project.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
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
 * 프로젝트 생성 요청(FR-PRJ-01). 단계형 화면 ①~⑤의 입력을 FE가 모아 마지막에 한 번 보낸다.
 * PM은 받지 않는다(생성자 자동 지정). members·gateApproverUserId는 선택이다.
 * 코드는 영문·숫자·하이픈 2~20자로 받고 대문자로 바꿔 저장해, 대소문자만 다른 코드를 중복으로 본다.
 */
public record CreateProjectRequest(
	@NotBlank(message = "프로젝트 이름을 입력해 주세요")
	@Size(max = 100, message = "프로젝트 이름은 100자 이하로 입력해 주세요")
	String name,

	@NotBlank(message = "프로젝트 코드를 입력해 주세요")
	@Pattern(regexp = CODE_PATTERN, message = "프로젝트 코드는 영문·숫자로 시작하는 영문·숫자·하이픈(-) 2~20자로 입력해 주세요")
	String code,

	@NotNull(message = "시작일을 입력해 주세요")
	LocalDate startDate,

	@NotNull(message = "종료일을 입력해 주세요")
	LocalDate endDate,

	@NotNull(message = "주관 부서를 선택해 주세요")
	@Positive(message = "부서 ID를 확인해 주세요")
	Long departmentId,

	@NotNull(message = "공개 범위를 선택해 주세요")
	ProjectVisibility visibility,

	@NotNull(message = "운영 방식을 선택해 주세요")
	Methodology methodology,

	@NotNull(message = "부서 유형을 선택해 주세요")
	DepartmentType departmentType,

	@NotEmpty(message = "액터를 지정해 주세요")
	Set<@NotNull(message = "액터 값을 확인해 주세요") ProjectActor> actors,

	@Size(max = MAX_INITIAL_MEMBERS, message = "생성 시 초대할 수 있는 멤버는 최대 100명입니다")
	List<@NotNull(message = "멤버 값을 확인해 주세요") @Valid InitialMemberRequest> members,

	@Positive(message = "게이트 승인자 ID를 확인해 주세요")
	Long gateApproverUserId
) {

	public static final String CODE_PATTERN = "^[A-Za-z0-9][A-Za-z0-9-]{1,19}$";
	public static final int MAX_INITIAL_MEMBERS = 100;

	/** members를 생략하면 PM(생성자)만 있는 프로젝트가 된다. */
	public CreateProjectCommand toCommand() {
		return new CreateProjectCommand(
			name,
			normalizeCode(code),
			startDate,
			endDate,
			departmentId,
			visibility,
			methodology,
			departmentType,
			Set.copyOf(actors),
			members == null ? List.of() : members.stream().map(InitialMemberRequest::toCommand).toList(),
			gateApproverUserId
		);
	}

	public static String normalizeCode(final String code) {
		return code.toUpperCase(Locale.ROOT);
	}
}
