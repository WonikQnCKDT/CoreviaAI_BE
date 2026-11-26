package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 생성 후 프로젝트 설정 변경 종류(FR-PRJ-14). 한 요청에서 바뀐 항목마다 이력 한 행을 남긴다.
 */
public enum ProjectSettingChangeType {
	BASIC_INFO,
	METHODOLOGY,
	ACTORS,
	FEATURES,
	GATE_APPROVER
}
