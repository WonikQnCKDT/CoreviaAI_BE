package org.wonik.coreviaai_be.domain.project.entity.type;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

/**
 * 프로젝트 템플릿(FR-PRJ-01 ②, FR-PRJ-09). 템플릿은 생성 화면의 기본값만 정하고, 사용자는 생성 확정 전에
 * 운영 방식·액터·기능 모듈을 개별로 바꿀 수 있다. 그래서 서버는 템플릿과 다른 조합도 받아들이고, 템플릿은 기록용으로 저장한다.
 * <ul>
 *   <li>EMPTY: 단계·게이트 없음. IT 개발 외 일반 업무·회의록·일정·파일 관리용</li>
 *   <li>IT_PROJECT: 기본 5단계 이름·SW 액터·기능 모듈을 제안하며, 생성 전에 단계 구성을 바꿀 수 있음</li>
 * </ul>
 * 생성에 쓰는 기본값은 project_templates에 저장하고 관리자가 고친다(FR-PRJ-09). 이 enum은 템플릿 키와
 * 최초 시드 설명이다. SW·제조 세부 유형 템플릿은 후속 이슈에서 추가한다.
 */
@Getter
public enum ProjectTemplate {
	EMPTY(
		Methodology.NONE,
		List.of(),
		EnumSet.of(ProjectActor.PM),
		EnumSet.of(ProjectFeature.KANBAN, ProjectFeature.BOARD, ProjectFeature.MEETING, ProjectFeature.ISSUE,
			ProjectFeature.FILE)
	),
	IT_PROJECT(
		Methodology.WATERFALL,
		List.of("분석", "설계", "개발", "테스트", "이행"),
		EnumSet.of(ProjectActor.PM, ProjectActor.BUSINESS_OWNER, ProjectActor.DEVELOPER, ProjectActor.QA),
		EnumSet.of(ProjectFeature.WBS, ProjectFeature.KANBAN, ProjectFeature.BOARD, ProjectFeature.MEETING,
			ProjectFeature.DELIVERABLE, ProjectFeature.ISSUE, ProjectFeature.FILE)
	);

	private final Methodology defaultMethodology;
	private final List<String> defaultPhases;
	private final Set<ProjectActor> defaultActors;
	private final Set<ProjectFeature> defaultFeatures;

	ProjectTemplate(
		final Methodology defaultMethodology,
		final List<String> defaultPhases,
		final Set<ProjectActor> defaultActors,
		final Set<ProjectFeature> defaultFeatures
	) {
		this.defaultMethodology = defaultMethodology;
		this.defaultPhases = List.copyOf(defaultPhases);
		this.defaultActors = Collections.unmodifiableSet(defaultActors);
		this.defaultFeatures = Collections.unmodifiableSet(defaultFeatures);
	}
}
