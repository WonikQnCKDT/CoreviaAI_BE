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
 * 공통 코드 값(FR-ADM-07, codes 테이블).
 * <p>
 * 코드 값(code)과 라벨 키(labelKey)는 업무 데이터·화면이 참조하는 고정 식별자라 수정·삭제하지 않는다:
 * 컬럼을 updatable = false로 두고 바꾸는 메서드도 두지 않는다. 표시 텍스트는 라벨 키로 다국어 라벨(FR-ADM-06)에서 관리한다.
 * 관리자가 바꿀 수 있는 것은 정렬 순서와 사용 여부뿐이다. 그룹은 JPA 연관관계 없이 ID만 가진다.
 */
@Getter
@Entity
@Table(name = "codes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommonCode extends BaseTimeEntity {

	/** 그룹 코드 형식: 대문자로 시작하는 대문자·숫자·밑줄(enum 이름과 같은 형식), 50자 이하(group_code 컬럼). */
	public static final String GROUP_CODE_PATTERN = "^[A-Z][A-Z0-9_]{0,49}$";
	/**
	 * 코드 값 형식: 그룹 코드와 같되 40자 이하. 컬럼은 50자지만 라벨 키 "code.그룹.코드"가
	 * label_key(100자) 안에 들어가도록(5 + 50 + 1 + 40 = 96) 코드 값을 더 짧게 제한한다.
	 */
	public static final String CODE_PATTERN = "^[A-Z][A-Z0-9_]{0,39}$";
	public static final int MAX_SORT_ORDER = 9999;

	private static final String GROUP_LABEL_PREFIX = "codeGroup.";
	private static final String CODE_LABEL_PREFIX = "code.";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false, updatable = false)
	private Long groupId;

	@Column(name = "code", nullable = false, updatable = false, length = 50)
	private String code;

	@Column(name = "label_key", nullable = false, updatable = false, length = 100)
	private String labelKey;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "active", nullable = false)
	private boolean active;

	private CommonCode(final CodeGroup group, final String code, final int sortOrder) {
		this.groupId = group.getId();
		this.code = code;
		this.labelKey = codeLabelKey(group.getGroupCode(), code);
		this.sortOrder = sortOrder;
		this.active = true;
	}

	/** 새 코드는 사용 중(active) 상태로 만들고 라벨 키는 규칙대로 서버가 정한다. */
	public static CommonCode create(final CodeGroup group, final String code, final int sortOrder) {
		return new CommonCode(group, code, sortOrder);
	}

	/** 그룹 이름 라벨 키. 다국어 라벨(FR-ADM-06) 시드가 이 규칙으로 키를 만든다. */
	public static String groupLabelKey(final String groupCode) {
		return GROUP_LABEL_PREFIX + groupCode;
	}

	/** 코드 표시 텍스트 라벨 키. 다국어 라벨(FR-ADM-06) 시드가 이 규칙으로 키를 만든다. */
	public static String codeLabelKey(final String groupCode, final String code) {
		return CODE_LABEL_PREFIX + groupCode + "." + code;
	}

	public void changeSortOrder(final int sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void changeActive(final boolean active) {
		this.active = active;
	}
}
