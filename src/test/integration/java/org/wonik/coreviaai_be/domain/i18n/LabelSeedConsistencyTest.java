package org.wonik.coreviaai_be.domain.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;

/**
 * V9 라벨 시드가 V8 공통 코드의 라벨 키와 1:1로 맞는지 검증한다(코드 라벨 누락 방지).
 * 코드 그룹·코드를 마이그레이션으로 추가하면 같은 키의 라벨 시드도 함께 추가해야 이 테스트가 통과한다.
 * 테스트가 만든 TEST_ 그룹·코드와 test. 라벨은 비교에서 뺀다.
 */
@SpringBootTest
@ActiveProfiles("test")
class LabelSeedConsistencyTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private I18nLabelRepository i18nLabelRepository;

	@Test
	@DisplayName("코드 그룹·코드의 라벨 키 집합과 code·codeGroup 라벨 키 집합이 같다")
	void codeLabelKeysMatchOneToOne() {
		final Set<String> codeKeys = new HashSet<>(jdbcTemplate.queryForList(
			"SELECT label_key FROM code_groups WHERE group_code NOT LIKE 'TEST\\_%'"
				+ " UNION SELECT c.label_key FROM codes c JOIN code_groups g ON g.id = c.group_id"
				+ " WHERE g.group_code NOT LIKE 'TEST\\_%'",
			String.class));
		final Set<String> labelKeys = new HashSet<>(codeLabels().stream().map(I18nLabel::getLabelKey).toList());

		assertThat(codeKeys).hasSize(18);
		assertThat(labelKeys).isEqualTo(codeKeys);
	}

	@Test
	@DisplayName("코드 라벨 시드는 한국어·영어·번체 중국어 문구가 모두 있다")
	void codeLabelsTranslated() {
		assertThat(codeLabels()).isNotEmpty().allSatisfy(label -> {
			assertThat(label.getTextKo()).isNotBlank();
			assertThat(label.getTextEn()).isNotBlank();
			assertThat(label.getTextZhTw()).isNotBlank();
			assertThat(label.getLabelKey()).matches(I18nLabel.LABEL_KEY_PATTERN);
		});
	}

	private List<I18nLabel> codeLabels() {
		return i18nLabelRepository.findAll().stream()
			.filter(label -> label.getLabelKey().startsWith("code.") || label.getLabelKey().startsWith("codeGroup."))
			.filter(label -> !label.getLabelKey().startsWith("code.TEST_") && !label.getLabelKey().startsWith("codeGroup.TEST_"))
			.toList();
	}
}
