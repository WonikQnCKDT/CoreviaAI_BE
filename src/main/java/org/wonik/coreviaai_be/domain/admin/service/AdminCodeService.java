package org.wonik.coreviaai_be.domain.admin.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.code.repository.CodeGroupRepository;
import org.wonik.coreviaai_be.domain.code.repository.CommonCodeRepository;
import org.wonik.coreviaai_be.domain.code.service.result.GroupCodes;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 공통 코드 관리(FR-ADM-07).
 * <p>
 * 코드 값은 업무 데이터가 참조하는 고정 값이라 수정·삭제하지 않는다(그런 기능 자체를 두지 않는다). 관리자는 새 코드를 추가하거나
 * 정렬 순서·사용 여부만 바꾼다. 표시 텍스트는 라벨 키로 다국어 라벨(FR-ADM-06)에서 바꾼다.
 * 변경은 감사 로그와 같은 트랜잭션으로 남겨 이력을 보관한다(인수 기준 "변경 이력 보관").
 */
@Service
@RequiredArgsConstructor
public class AdminCodeService {

	private final CodeGroupRepository codeGroupRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final AuditLogger auditLogger;

	@Transactional(readOnly = true)
	public List<CodeGroup> findGroups() {
		return codeGroupRepository.findAllByOrderByIdAsc();
	}

	/** 사용하지 않는 코드까지 정렬 순서대로 준다. */
	@Transactional(readOnly = true)
	public GroupCodes findCodes(final String groupCode) {
		final CodeGroup group = getGroup(groupCode);
		return new GroupCodes(group, commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(group.getId()));
	}

	/** sortOrder가 없으면 그룹의 마지막 순서 다음에 둔다. 같은 그룹에 같은 코드 값이 있으면 409(재전송도 하나만 생김). */
	@Transactional
	public CommonCode create(final AuditActor actor, final String groupCode, final String code, final Integer sortOrder) {
		final CodeGroup group = getGroup(groupCode);
		if (commonCodeRepository.existsByGroupIdAndCode(group.getId(), code)) {
			throw new CustomException(ErrorCode.DUPLICATE_CODE);
		}
		final int order = sortOrder != null ? sortOrder : nextSortOrder(group.getId());

		final CommonCode saved = saveAndFlush(CommonCode.create(group, code, order));
		auditLogger.record(actor, AuditAction.CODE_CREATED, AuditTargetType.CODE, saved.getId(),
			AuditChanges.create()
				.value("groupCode", groupCode)
				.value("code", code)
				.value("labelKey", saved.getLabelKey())
				.value("sortOrder", order));
		return saved;
	}

	/** 같은 값이면 아무것도 바꾸지 않아, 재전송된 요청이 이력을 늘리지 않는다. */
	@Transactional
	public CommonCode update(
		final AuditActor actor,
		final String groupCode,
		final String code,
		final int sortOrder,
		final boolean active
	) {
		final CodeGroup group = getGroup(groupCode);
		final CommonCode commonCode = commonCodeRepository.findByGroupIdAndCode(group.getId(), code)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_NOT_FOUND));
		if (commonCode.getSortOrder() == sortOrder && commonCode.isActive() == active) {
			return commonCode;
		}

		final AuditChanges changes = AuditChanges.create()
			.value("groupCode", groupCode)
			.value("code", code)
			.change("sortOrder", commonCode.getSortOrder(), sortOrder)
			.change("active", commonCode.isActive(), active);
		commonCode.changeSortOrder(sortOrder);
		commonCode.changeActive(active);
		commonCodeRepository.flush();
		auditLogger.record(actor, AuditAction.CODE_CHANGED, AuditTargetType.CODE, commonCode.getId(), changes);
		return commonCode;
	}

	private CodeGroup getGroup(final String groupCode) {
		return codeGroupRepository.findByGroupCode(groupCode)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_GROUP_NOT_FOUND));
	}

	private int nextSortOrder(final Long groupId) {
		final int next = commonCodeRepository.findMaxSortOrder(groupId).map(max -> max + 1).orElse(1);
		return Math.min(next, CommonCode.MAX_SORT_ORDER);
	}

	/** 사전 검사를 통과한 뒤 같은 코드가 동시에 저장된 경우, DB 유니크 제약 위반을 중복 오류로 바꾼다. */
	private CommonCode saveAndFlush(final CommonCode code) {
		try {
			return commonCodeRepository.saveAndFlush(code);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_CODE, e);
		}
	}
}
