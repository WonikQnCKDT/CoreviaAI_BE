package org.wonik.coreviaai_be.domain.project.entity.type;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import lombok.Getter;

/**
 * 프로젝트 템플릿(FR-PRJ-01 ②, FR-PRJ-09). 템플릿은 생성 화면의 기본값만 정하고, 사용자는 생성 확정 전에
 * 운영 방식·액터·기능 모듈을 개별로 바꿀 수 있다. 그래서 서버는 템플릿과 다른 조합도 받아들이고, 템플릿은 기록용으로 저장한다.
 * <ul>
 *   <li>EMPTY: 단계·게이트 없음. IT 개발 외 일반 업무·회의록·일정·파일 관리용</li>
 *   <li>IT_PROJECT: 폭포수 5단계·게이트·SW 액터·기능 모듈 자동 세팅</li>
 * </ul>
 * SW·제조 세부 유형 템플릿과 관리자 템플릿 관리 화면(FR-PRJ-09)은 후속 이슈에서 추가한다.
 */
@Getter
public enum ProjectTemplate {
	EMPTY(
		Methodology.NONE,
		EnumSet.of(ProjectActor.PM),
		EnumSet.of(ProjectFeature.KANBAN, ProjectFeature.BOARD, ProjectFeature.MEETING, ProjectFeature.ISSUE,
			ProjectFeature.FILE)
	),
	IT_PROJECT(
		Methodology.WATERFALL,
		EnumSet.of(ProjectActor.PM, ProjectActor.BUSINESS_OWNER, ProjectActor.DEVELOPER, ProjectActor.QA),
		EnumSet.of(ProjectFeature.WBS, ProjectFeature.KANBAN, ProjectFeature.BOARD, ProjectFeature.MEETING,
			ProjectFeature.DELIVERABLE, ProjectFeature.ISSUE, ProjectFeature.FILE)
	);

	private final Methodology defaultMethodology;
	private final Set<ProjectActor> defaultActors;
	private final Set<ProjectFeature> defaultFeatures;

	ProjectTemplate(
		final Methodology defaultMethodology,
		final Set<ProjectActor> defaultActors,
		final Set<ProjectFeature> defaultFeatures
	) {
		this.defaultMethodology = defaultMethodology;
		this.defaultActors = Collections.unmodifiableSet(defaultActors);
		this.defaultFeatures = Collections.unmodifiableSet(defaultFeatures);
	}
}
