package org.wonik.coreviaai_be.domain.project.entity.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * 생성 선택지(운영 방식·기능 모듈·템플릿)의 MVP 범위 규칙(단위 테스트). FE 선택지 API가 이 값을 그대로 내려준다.
 */
class ProjectTypeRulesTest {

	@Test
	@DisplayName("운영 방식: 단계 없음·폭포수만 활성이고, 단계·게이트는 폭포수만 쓴다")
	void methodology() {
		assertThat(Arrays.stream(Methodology.values()).filter(Methodology::isEnabled))
			.containsExactly(Methodology.NONE, Methodology.WATERFALL);
		assertThat(Arrays.stream(Methodology.values()).filter(Methodology::isUsesPhases))
			.containsExactly(Methodology.WATERFALL);
	}

	@Test
	@DisplayName("기능 모듈: 산출물·게이트는 폭포수에서만, Git 연동은 어디서도 선택할 수 없다")
	void featureSelectable() {
		assertThat(ProjectFeature.DELIVERABLE.selectableFor(Methodology.WATERFALL)).isTrue();
		assertThat(ProjectFeature.DELIVERABLE.selectableFor(Methodology.NONE)).isFalse();
		assertThat(ProjectFeature.GIT.selectableFor(Methodology.WATERFALL)).isFalse();
		assertThat(ProjectFeature.KANBAN.selectableFor(Methodology.NONE)).isTrue();
	}

	@ParameterizedTest
	@EnumSource(ProjectTemplate.class)
	@DisplayName("템플릿 기본값은 그 자체로 생성 규칙을 통과한다(PM 포함, 운영 방식에서 켤 수 있는 모듈만)")
	void templateDefaultsAreConsistent(final ProjectTemplate template) {
		assertThat(template.getDefaultMethodology().isEnabled()).isTrue();
		assertThat(template.getDefaultActors()).contains(ProjectActor.PM);
		assertThat(template.getDefaultActors()).allMatch(actor -> actor.selectableFor(DepartmentType.SW));
		assertThat(template.getDefaultFeatures())
			.allMatch(feature -> feature.selectableFor(template.getDefaultMethodology()));
	}

	@Test
	@DisplayName("빈 템플릿은 단계 없음, IT 프로젝트 템플릿은 폭포수·SW 액터 전체를 기본값으로 쓴다")
	void templateDefaults() {
		assertThat(ProjectTemplate.EMPTY.getDefaultMethodology()).isEqualTo(Methodology.NONE);
		assertThat(ProjectTemplate.EMPTY.getDefaultFeatures()).doesNotContain(ProjectFeature.DELIVERABLE, ProjectFeature.WBS);
		assertThat(ProjectTemplate.IT_PROJECT.getDefaultMethodology()).isEqualTo(Methodology.WATERFALL);
		assertThat(ProjectTemplate.IT_PROJECT.getDefaultActors()).containsExactlyInAnyOrder(
			ProjectActor.PM, ProjectActor.BUSINESS_OWNER, ProjectActor.DEVELOPER, ProjectActor.QA);
	}
}
