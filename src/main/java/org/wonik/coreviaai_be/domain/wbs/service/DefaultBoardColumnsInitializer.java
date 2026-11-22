package org.wonik.coreviaai_be.domain.wbs.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.structure.ProjectDefaultDataInitializer;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.repository.BoardColumnRepository;

import lombok.RequiredArgsConstructor;

/**
 * 새 프로젝트에 칸반 기본 컬럼 4개(대기·진행·검토·완료)를 만든다(FR-TSK-02 '상태별 컬럼'). 템플릿·운영 방식·KANBAN 모듈 사용 여부와
 * 관계없이 만든다: 작업 상태가 컬럼으로 정해지고, 모듈을 나중에 켜도 바로 쓸 수 있어야 한다(FR-PRJ-06·14). V12 백필과 같은 구성이다.
 */
@Component
@RequiredArgsConstructor
public class DefaultBoardColumnsInitializer implements ProjectDefaultDataInitializer {

	private final BoardColumnRepository boardColumnRepository;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void initialize(final Project project) {
		boardColumnRepository.saveAll(BoardColumn.defaults(project.getId()));
	}
}
