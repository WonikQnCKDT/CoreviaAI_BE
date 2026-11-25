package org.wonik.coreviaai_be.domain.menu.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.global.common.entity.BaseTimeEntity;
import org.wonik.global.common.type.SystemRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메뉴(FR-ADM-08, menus). 화면 문구는 labelKey가 가리키는 다국어 라벨이고, 메뉴 키는 바꾸지 않는다.
 * requiredRole이 null이면 로그인한 모든 시스템 역할에 보인다.
 */
@Getter
@Entity
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "menu_key", nullable = false, updatable = false, length = 50)
	private String menuKey;

	@Column(name = "label_key", nullable = false, updatable = false, length = 100)
	private String labelKey;

	@Column(name = "url_path", length = 200)
	private String urlPath;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "required_role", length = 30)
	private SystemRole requiredRole;

	@Column(name = "active", nullable = false)
	private boolean active;

	public boolean visibleTo(final SystemRole role) {
		return active && (requiredRole == null || requiredRole == role);
	}
}
