package org.wonik.coreviaai_be.domain.code;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.code.repository.CodeGroupRepository;
import org.wonik.coreviaai_be.domain.code.repository.CommonCodeRepository;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * V8 시드(코드 테이블)와 코드 enum이 어긋나지 않는지 검증한다. enum에 값을 추가·변경하면 새 마이그레이션으로 코드를 맞춰야 한다.
 * 화면은 코드 테이블을, 업무 로직은 enum을 쓰므로 둘이 다르면 화면에 없는 상태가 저장되거나 저장할 수 없는 상태가 보인다.
 */
@SpringBootTest
@ActiveProfiles("test")
class CodeSeedConsistencyTest {

	@Autowired
	private CodeGroupRepository codeGroupRepository;
	@Autowired
	private CommonCodeRepository commonCodeRepository;

	@Test
	@DisplayName("TASK_STATUS 코드는 WbsTaskStatus 값과 같은 집합·순서다")
	void taskStatusMatchesEnum() {
		assertThat(seededCodes("TASK_STATUS")).containsExactlyElementsOf(names(WbsTaskStatus.values()));
	}

	@Test
	@DisplayName("PROJECT_STATUS 코드는 ProjectStatus 값과 같은 집합·순서다")
	void projectStatusMatchesEnum() {
		assertThat(seededCodes("PROJECT_STATUS")).containsExactlyElementsOf(names(ProjectStatus.values()));
	}

	@Test
	@DisplayName("RISK_LEVEL은 정상·주의·위험, REQUEST_RESULT는 SUCCESS·FAILURE로 시드된다")
	void otherGroupsSeeded() {
		assertThat(seededCodes("RISK_LEVEL")).containsExactly("NORMAL", "CAUTION", "DANGER");
		assertThat(seededCodes("REQUEST_RESULT")).containsExactly("SUCCESS", "FAILURE");
	}

	@Test
	@DisplayName("시드 그룹·코드의 라벨 키는 codeGroup.<그룹>·code.<그룹>.<코드> 규칙을 따르고 모두 사용 중이다")
	void labelKeysFollowRule() {
		for (final String groupCode : List.of("RISK_LEVEL", "TASK_STATUS", "REQUEST_RESULT", "PROJECT_STATUS")) {
			final CodeGroup group = codeGroupRepository.findByGroupCode(groupCode).orElseThrow();
			assertThat(group.getLabelKey()).isEqualTo(CommonCode.groupLabelKey(groupCode));
			assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(group.getId()))
				.isNotEmpty()
				.allSatisfy(code -> {
					assertThat(code.getLabelKey()).isEqualTo(CommonCode.codeLabelKey(groupCode, code.getCode()));
					assertThat(code.getCode()).matches(CommonCode.CODE_PATTERN);
					assertThat(code.isActive()).isTrue();
				});
		}
	}

	private List<String> seededCodes(final String groupCode) {
		final CodeGroup group = codeGroupRepository.findByGroupCode(groupCode).orElseThrow();
		return commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(group.getId()).stream()
			.map(CommonCode::getCode)
			.toList();
	}

	private static List<String> names(final Enum<?>[] values) {
		return Arrays.stream(values).map(Enum::name).toList();
	}
}
