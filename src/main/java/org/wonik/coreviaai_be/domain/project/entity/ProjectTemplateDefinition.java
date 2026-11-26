package org.wonik.coreviaai_be.domain.project.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 생성 화면 템플릿 기본값(FR-PRJ-09). 프로젝트는 생성 시 이 값을 복사하므로, 여기서 고쳐도 기존 프로젝트는 바뀌지 않는다.
 * defaultConfig는 서버가 검증한 JSON이다. 키 추가(SW·제조 세부 유형)는 후속이다.
 */
@Getter
@Entity
@Table(name = "project_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTemplateDefinition extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "template_key", nullable = false, unique = true, length = 50)
	private ProjectTemplate templateKey;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "methodology", nullable = false, length = 20)
	private Methodology methodology;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "department_type", length = 20)
	private DepartmentType departmentType;

	/** JSON 텍스트. JSON 타입 매핑은 값을 한 번 더 감싸므로 문자열로 읽고 서버가 파싱한다. */
	@Column(name = "default_config", nullable = false, length = 4000)
	private String defaultConfig;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	public void replaceDefaults(
		final String name,
		final Methodology methodology,
		final DepartmentType departmentType,
		final String defaultConfig
	) {
		this.name = name;
		this.methodology = methodology;
		this.departmentType = departmentType;
		this.defaultConfig = defaultConfig;
	}
}
