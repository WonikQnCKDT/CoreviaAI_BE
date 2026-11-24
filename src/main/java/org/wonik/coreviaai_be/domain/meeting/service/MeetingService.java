package org.wonik.coreviaai_be.domain.meeting.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.meeting.entity.ActionItem;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting.ActionItemValue;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting.AttendeeValue;
import org.wonik.coreviaai_be.domain.meeting.entity.MeetingAttendee;
import org.wonik.coreviaai_be.domain.meeting.entity.MeetingDecision;
import org.wonik.coreviaai_be.domain.meeting.entity.type.ActionItemStatus;
import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;
import org.wonik.coreviaai_be.domain.meeting.pagination.MeetingCursor;
import org.wonik.coreviaai_be.domain.meeting.policy.MeetingAccessPolicy;
import org.wonik.coreviaai_be.domain.meeting.repository.MeetingRepository;
import org.wonik.coreviaai_be.domain.meeting.service.command.MeetingActionItemCommand;
import org.wonik.coreviaai_be.domain.meeting.service.command.MeetingAttendeeCommand;
import org.wonik.coreviaai_be.domain.meeting.service.command.SaveMeetingCommand;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingActionItemResult;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingAttendeeResult;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingDecisionResult;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingDetailResult;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingSummaryResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

/** FR-DOC-01 회의록 작성·수정·검색과 작성자/PM 권한을 한 트랜잭션 경계에서 처리한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {

	public static final int MAX_ATTENDEES = 100;
	public static final int MAX_DECISIONS = 100;
	public static final int MAX_ACTION_ITEMS = 100;
	private static final String LIKE_ESCAPE = "\\";

	private final MeetingRepository meetingRepository;
	private final MeetingAccessPolicy accessPolicy;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;
	private final EntityManager entityManager;

	public CursorPage<MeetingSummaryResult> findPage(
		final Long viewerId,
		final Long projectId,
		final String keyword,
		final String cursor,
		final int size
	) {
		accessPolicy.getReadableProject(viewerId, projectId);
		final String normalizedKeyword = normalizeKeyword(keyword);
		final MeetingCursor decoded = MeetingCursor.decode(cursor, projectId, normalizedKeyword);
		final List<Meeting> fetched = meetingRepository.searchPage(projectId,
			decoded == null ? null : decoded.heldAt(), decoded == null ? null : decoded.id(),
			likePattern(normalizedKeyword), PageRequest.of(0, size + 1));
		final boolean hasNext = fetched.size() > size;
		final List<Meeting> page = hasNext ? List.copyOf(fetched.subList(0, size)) : List.copyOf(fetched);
		final Map<Long, User> users = usersById(page.stream().map(Meeting::getAuthorUserId).collect(Collectors.toSet()));
		final List<MeetingSummaryResult> items = page.stream()
			.map(meeting -> MeetingSummaryResult.from(meeting, users.get(meeting.getAuthorUserId()).getName()))
			.toList();
		final Meeting last = hasNext ? page.get(page.size() - 1) : null;
		final String nextCursor = last == null ? null
			: MeetingCursor.encode(projectId, normalizedKeyword, last.getHeldAt(), last.getId());
		return new CursorPage<>(items, nextCursor, hasNext);
	}

	public MeetingDetailResult get(final Long viewerId, final Long projectId, final Long meetingId) {
		accessPolicy.getReadableProject(viewerId, projectId);
		return detail(getMeeting(projectId, meetingId));
	}

	@Transactional
	public MeetingDetailResult create(
		final Long authorUserId,
		final Long projectId,
		final SaveMeetingCommand command
	) {
		accessPolicy.getWritableProject(authorUserId, projectId);
		final PreparedMeeting prepared = prepare(command);
		final Meeting meeting = Meeting.create(projectId, prepared.title(), command.heldAt(), command.content(),
			authorUserId);
		apply(meeting, prepared);
		return detail(meetingRepository.saveAndFlush(meeting));
	}

	@Transactional
	public MeetingDetailResult update(
		final Long viewerId,
		final Long projectId,
		final Long meetingId,
		final long version,
		final SaveMeetingCommand command
	) {
		accessPolicy.getReadableProject(viewerId, projectId);
		final Meeting meeting = getMeetingForUpdate(projectId, meetingId);
		accessPolicy.requireEditor(viewerId, meeting);
		if (meeting.getVersion() != version) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		requireReplaceable(meeting);
		final PreparedMeeting prepared = prepare(command);
		if (Objects.equals(meeting.getTitle(), prepared.title())
			&& Objects.equals(meeting.getHeldAt(), command.heldAt())
			&& Objects.equals(meeting.getContent(), command.content())) {
			entityManager.lock(meeting, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
		}
		meeting.changeDetails(prepared.title(), command.heldAt(), command.content());
		apply(meeting, prepared);
		meetingRepository.flush();
		return detail(meeting);
	}

	@Transactional
	public void delete(final AuditActor actor, final Long projectId, final Long meetingId) {
		accessPolicy.getReadableProject(actor.userId(), projectId);
		final Meeting meeting = getMeetingForUpdate(projectId, meetingId);
		accessPolicy.requireEditor(actor.userId(), meeting);
		requireReplaceable(meeting);
		meetingRepository.delete(meeting);
		meetingRepository.flush();
		auditLogger.record(actor, AuditAction.MEETING_DELETED, AuditTargetType.MEETING, meetingId,
			AuditChanges.create().value("projectId", projectId).value("title", meeting.getTitle()));
	}

	private PreparedMeeting prepare(final SaveMeetingCommand command) {
		if (command == null || command.heldAt() == null || command.title() == null
			|| command.attendees() == null || command.decisions() == null || command.actionItems() == null
			|| command.attendees().size() > MAX_ATTENDEES || command.decisions().size() > MAX_DECISIONS
			|| command.actionItems().size() > MAX_ACTION_ITEMS
			|| command.content() != null && command.content().length() > Meeting.CONTENT_MAX_LENGTH) {
			throw new CustomException(ErrorCode.INVALID_MEETING_CONTENT);
		}
		final String title = command.title().strip();
		if (title.isEmpty() || title.length() > Meeting.TITLE_MAX_LENGTH) {
			throw new CustomException(ErrorCode.INVALID_MEETING_CONTENT);
		}

		final List<AttendeeValue> attendees = prepareAttendees(command.attendees());
		final List<String> decisions = command.decisions().stream().map(MeetingService::meetingItem).toList();
		final List<ActionItemValue> actionItems = command.actionItems().stream()
			.map(item -> new ActionItemValue(meetingItem(item.content()), item.assigneeUserId(), item.dueDate()))
			.toList();
		final Set<Long> referencedUsers = attendees.stream().map(AttendeeValue::userId)
			.filter(id -> id != null).collect(Collectors.toSet());
		actionItems.stream().map(ActionItemValue::assigneeUserId).filter(id -> id != null).forEach(referencedUsers::add);
		validateActiveUsers(referencedUsers);
		return new PreparedMeeting(title, attendees, decisions, actionItems);
	}

	private static List<AttendeeValue> prepareAttendees(final List<MeetingAttendeeCommand> commands) {
		final List<AttendeeValue> values = new ArrayList<>();
		final Set<Long> userIds = new HashSet<>();
		final Set<String> externalNames = new HashSet<>();
		for (final MeetingAttendeeCommand command : commands) {
			if (command == null || (command.userId() == null) == (command.externalName() == null)) {
				throw new CustomException(ErrorCode.INVALID_MEETING_ATTENDEE);
			}
			if (command.userId() != null) {
				if (command.userId() <= 0 || !userIds.add(command.userId())) {
					throw new CustomException(ErrorCode.INVALID_MEETING_ATTENDEE);
				}
				values.add(new AttendeeValue(command.userId(), null));
				continue;
			}
			final String externalName = command.externalName().strip();
			if (externalName.isEmpty() || externalName.length() > MeetingAttendee.EXTERNAL_NAME_MAX_LENGTH
				|| !externalNames.add(externalName.toLowerCase(Locale.ROOT))) {
				throw new CustomException(ErrorCode.INVALID_MEETING_ATTENDEE);
			}
			values.add(new AttendeeValue(null, externalName));
		}
		return List.copyOf(values);
	}

	private void validateActiveUsers(final Set<Long> userIds) {
		if (userIds.isEmpty()) {
			return;
		}
		final List<User> users = userRepository.findAllById(userIds);
		if (users.size() != userIds.size() || users.stream().anyMatch(user -> user.getStatus() != UserStatus.ACTIVE)) {
			throw new CustomException(ErrorCode.INVALID_MEETING_ATTENDEE);
		}
	}

	private static String meetingItem(final String content) {
		if (content == null) {
			throw new CustomException(ErrorCode.INVALID_MEETING_CONTENT);
		}
		final String normalized = content.strip();
		if (normalized.isEmpty() || normalized.length() > MeetingDecision.CONTENT_MAX_LENGTH) {
			throw new CustomException(ErrorCode.INVALID_MEETING_CONTENT);
		}
		return normalized;
	}

	private static void apply(final Meeting meeting, final PreparedMeeting prepared) {
		meeting.replaceAttendees(prepared.attendees());
		meeting.replaceDecisions(prepared.decisions());
		meeting.replaceActionItems(prepared.actionItems());
	}

	private Meeting getMeeting(final Long projectId, final Long meetingId) {
		return meetingRepository.findByIdAndProjectId(meetingId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
	}

	private Meeting getMeetingForUpdate(final Long projectId, final Long meetingId) {
		return meetingRepository.findForUpdate(meetingId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));
	}

	/** 후속 기능이 생성한 항목의 출처·업무 연결을 전체 교체로 잃지 않도록 차단한다. */
	private static void requireReplaceable(final Meeting meeting) {
		if (meeting.getDecisions().stream().anyMatch(item -> item.getSource() != MeetingItemSource.MANUAL)
			|| meeting.getActionItems().stream().anyMatch(item -> item.getSource() != MeetingItemSource.MANUAL
				|| item.getStatus() != ActionItemStatus.OPEN || item.getTaskId() != null
				|| item.getApprovedBy() != null || item.getApprovedAt() != null)) {
			throw new CustomException(ErrorCode.MEETING_ITEMS_PROTECTED);
		}
	}

	private MeetingDetailResult detail(final Meeting meeting) {
		final Set<Long> userIds = new HashSet<>();
		userIds.add(meeting.getAuthorUserId());
		meeting.getAttendees().stream().map(MeetingAttendee::getUserId).filter(id -> id != null).forEach(userIds::add);
		meeting.getActionItems().stream().map(ActionItem::getAssigneeUserId).filter(id -> id != null)
			.forEach(userIds::add);
		final Map<Long, User> users = usersById(userIds);
		final List<MeetingAttendeeResult> attendees = meeting.getAttendees().stream().map(attendee -> {
			final boolean external = attendee.getUserId() == null;
			final String name = external ? attendee.getExternalName() : users.get(attendee.getUserId()).getName();
			return new MeetingAttendeeResult(attendee.getId(), attendee.getUserId(), name, external);
		}).toList();
		final List<MeetingDecisionResult> decisions = meeting.getDecisions().stream()
			.map(decision -> new MeetingDecisionResult(decision.getId(), decision.getContent(),
				decision.getSortOrder(), decision.getSource()))
			.toList();
		final List<MeetingActionItemResult> actionItems = meeting.getActionItems().stream().map(item ->
			new MeetingActionItemResult(item.getId(), item.getContent(), item.getAssigneeUserId(),
				item.getAssigneeUserId() == null ? null : users.get(item.getAssigneeUserId()).getName(),
				item.getDueDate(), item.getStatus(), item.getSource(), item.getTaskId())).toList();
		return new MeetingDetailResult(meeting.getId(), meeting.getProjectId(), meeting.getTitle(), meeting.getHeldAt(),
			meeting.getContent(), meeting.getAuthorUserId(), users.get(meeting.getAuthorUserId()).getName(),
			meeting.getVersion(), attendees, decisions, actionItems, meeting.getCreatedAt(), meeting.getUpdatedAt());
	}

	private Map<Long, User> usersById(final Set<Long> userIds) {
		return userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
	}

	private static String normalizeKeyword(final String keyword) {
		return keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
	}

	private static String likePattern(final String normalizedKeyword) {
		final String escaped = normalizedKeyword.replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
			.replace("%", LIKE_ESCAPE + "%").replace("_", LIKE_ESCAPE + "_");
		return "%" + escaped + "%";
	}

	private record PreparedMeeting(
		String title,
		List<AttendeeValue> attendees,
		List<String> decisions,
		List<ActionItemValue> actionItems
	) {
	}
}
