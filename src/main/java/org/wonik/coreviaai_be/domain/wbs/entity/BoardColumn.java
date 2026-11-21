package org.wonik.coreviaai_be.domain.wbs.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.global.common.entity.BaseTimeEntity;

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
 * 칸반 보드 컬럼(FR-TSK-02, V12). 카드(업무 = WBS 작업·하위작업)를 이 컬럼으로 옮기면 카드 상태가 {@link #statusCategory}가 된다.
 * <p>
 * 이름이 null이면 기본 이름이다. 기본 이름은 사용자 입력이 아니라 상태 코드 표시 문구라서 저장하지 않고
 * 공통 코드 라벨({@link #defaultLabelKey()})을 다국어로 보여 준다. 사용자가 붙인 이름은 입력값 그대로 보여 준다(라벨 키 아님).
 * <p>
 * 매핑 상태는 만든 뒤 바꾸지 않는다(카드 상태와 어긋나지 않게). 버전 컬럼이 없고(05 ERD) 변경은 프로젝트 행 잠금으로 차례로 처리한다.
 */
@Getter
@Entity
@Table(name = "board_columns")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardColumn extends BaseTimeEntity {

	public static final int NAME_MAX_LENGTH = 50;
	public static final int MAX_WIP_LIMIT = 999;
	/** 기본 컬럼 이름 라벨의 공통 코드 그룹(V8 시드). */
	public static final String STATUS_CODE_GROUP = "TASK_STATUS";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "name", length = NAME_MAX_LENGTH)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status_category", nullable = false, length = 20)
	private WbsTaskStatus statusCategory;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "wip_limit")
	private Integer wipLimit;

	private BoardColumn(
		final Long projectId,
		final String name,
		final WbsTaskStatus statusCategory,
		final int sortOrder,
		final Integer wipLimit
	) {
		this.projectId = projectId;
		this.name = name;
		this.statusCategory = statusCategory;
		this.sortOrder = sortOrder;
		this.wipLimit = wipLimit;
	}

	public static BoardColumn of(
		final Long projectId,
		final String name,
		final WbsTaskStatus statusCategory,
		final int sortOrder,
		final Integer wipLimit
	) {
		return new BoardColumn(projectId, name, statusCategory, sortOrder, wipLimit);
	}

	/** 새 프로젝트의 기본 컬럼: 대기·진행·검토·완료(상태 코드 순서), 기본 이름, WIP 한도 없음. V12 백필과 같다. */
	public static List<BoardColumn> defaults(final Long projectId) {
		final WbsTaskStatus[] statuses = WbsTaskStatus.values();
		final List<BoardColumn> columns = new ArrayList<>(statuses.length);
		for (int i = 0; i < statuses.length; i++) {
			columns.add(new BoardColumn(projectId, null, statuses[i], i + 1, null));
		}
		return columns;
	}

	/** 기본 이름을 쓰는 컬럼이면 표시 문구 라벨 키(code.TASK_STATUS.상태), 이름이 있으면 null. */
	public String defaultLabelKey() {
		return name == null ? CommonCode.codeLabelKey(STATUS_CODE_GROUP, statusCategory.name()) : null;
	}

	/** 카드 수가 WIP 한도를 넘었는지. 한도와 같으면 넘지 않은 것이다. 한도가 없으면 항상 false. */
	public boolean isWipExceeded(final int cardCount) {
		return wipLimit != null && cardCount > wipLimit;
	}

	public boolean hasSameSettings(final String name, final Integer wipLimit) {
		return Objects.equals(this.name, name) && Objects.equals(this.wipLimit, wipLimit);
	}

	/** 이름(null이면 기본 이름)과 WIP 한도(null이면 없음)를 바꾼다. 값 검증은 요청 경계에서 한다. */
	public void changeSettings(final String name, final Integer wipLimit) {
		this.name = name;
		this.wipLimit = wipLimit;
	}

	public void changeSortOrder(final int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
