package org.wonik.coreviaai_be.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminHistoryResult;
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
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 공통 코드 관리(FR-ADM-07).
 * <p>
 * 코드 값은 업무 데이터가 참조하는 고정 값이다(인수 기준: 코드 값 수정·삭제 불가). 코드 값의 추가·삭제와 사용 여부(active)
 * 변경은 기능 수정이라 마이그레이션으로만 한다. 관리자는 정렬 순서만 바꾸고, 표시 텍스트는 라벨 키로 다국어 라벨(FR-ADM-06)에서 바꾼다.
 * 변경은 감사 로그와 같은 트랜잭션으로 남겨 이력을 보관한다(인수 기준 "변경 이력 보관").
 */
@Service
@RequiredArgsConstructor
public class AdminCodeService {

	private final CodeGroupRepository codeGroupRepository;
	private final CommonCodeRepository commonCodeRepository;
	private final AuditLogger auditLogger;
	private final AdminAuditHistoryReader auditHistoryReader;

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

	/** 정렬 순서를 바꾼다. 같은 값이면 아무것도 바꾸지 않아, 재전송된 요청이 이력을 늘리지 않는다. */
	@Transactional
	public CommonCode changeSortOrder(final AuditActor actor, final String groupCode, final String code, final int sortOrder) {
		final CodeGroup group = getGroup(groupCode);
		final CommonCode commonCode = commonCodeRepository.findByGroupIdAndCode(group.getId(), code)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_NOT_FOUND));
		if (commonCode.getSortOrder() == sortOrder) {
			return commonCode;
		}

		final AuditChanges changes = AuditChanges.create()
			.value("groupCode", groupCode)
			.value("code", code)
			.change("sortOrder", commonCode.getSortOrder(), sortOrder);
		commonCode.changeSortOrder(sortOrder);
		commonCodeRepository.flush();
		auditLogger.record(actor, AuditAction.CODE_CHANGED, AuditTargetType.CODE, commonCode.getId(), changes);
		return commonCode;
	}

	/** 코드 변경 이력(최신순, 커서 페이지). */
	@Transactional(readOnly = true)
	public CursorPage<AdminHistoryResult> findHistories(
		final String groupCode,
		final String code,
		final CursorPageRequest page
	) {
		final CodeGroup group = getGroup(groupCode);
		final CommonCode commonCode = commonCodeRepository.findByGroupIdAndCode(group.getId(), code)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_NOT_FOUND));
		return auditHistoryReader.findByTarget(AuditTargetType.CODE, commonCode.getId(), page);
	}

	private CodeGroup getGroup(final String groupCode) {
		return codeGroupRepository.findByGroupCode(groupCode)
			.orElseThrow(() -> new CustomException(ErrorCode.CODE_GROUP_NOT_FOUND));
	}
}
