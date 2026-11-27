package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;

/**
 * 템플릿 default_config를 서버 필드로 읽은 값. 사용자 JSON을 그대로 신뢰하지 않고, 저장 전에 검증한 값만 다시 쓴다.
 *
 * @param requiredDeliverables 단계별 필수 산출물 이름. 산출물 도메인 테이블은 아직 없어 생성 시 행으로 복사하지 않는다.
 */
public record TemplateDefaults(
	List<String> phases,
	List<ProjectActor> actors,
	List<ProjectFeature> features,
	List<RequiredDeliverable> requiredDeliverables
) {

	public record RequiredDeliverable(String phaseName, String name) {
	}
}
