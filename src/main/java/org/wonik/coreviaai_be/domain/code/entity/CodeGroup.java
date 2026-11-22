package org.wonik.coreviaai_be.domain.code.entity;

import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 코드 그룹(FR-ADM-07). 그룹은 마이그레이션으로만 추가하므로 생성·수정 메서드가 없다.
 * 그룹 코드와 라벨 키는 화면·코드가 참조하는 식별자라 바꾸지 않는다(updatable = false).
 */
@Getter
@Entity
@Table(name = "code_groups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_code", nullable = false, updatable = false, length = 50)
	private String groupCode;

	@Column(name = "label_key", nullable = false, updatable = false, length = 100)
	private String labelKey;
}
