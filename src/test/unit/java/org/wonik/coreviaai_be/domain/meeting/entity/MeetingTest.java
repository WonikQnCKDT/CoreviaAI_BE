package org.wonik.coreviaai_be.domain.meeting.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting.ActionItemValue;
import org.wonik.coreviaai_be.domain.meeting.entity.Meeting.AttendeeValue;
import org.wonik.coreviaai_be.domain.meeting.entity.type.ActionItemStatus;
import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;

class MeetingTest {

	@Test
	void replacesOwnedCollectionsAndKeepsManualDefaultsAndDecisionOrder() {
		final Meeting meeting = Meeting.create(10L, "주간 회의", Instant.parse("2026-11-24T01:00:00Z"),
			"본문", 20L);

		meeting.replaceAttendees(List.of(new AttendeeValue(21L, null), new AttendeeValue(null, "외부 참석자")));
		meeting.replaceDecisions(List.of("첫 결정", "두 번째 결정"));
		meeting.replaceActionItems(List.of(new ActionItemValue("검토", 21L, LocalDate.of(2026, 11, 30))));

		assertThat(meeting.getAttendees()).hasSize(2);
		assertThat(meeting.getAttendees().get(0).getUserId()).isEqualTo(21L);
		assertThat(meeting.getAttendees().get(1).getExternalName()).isEqualTo("외부 참석자");
		assertThat(meeting.getDecisions()).extracting(MeetingDecision::getSortOrder).containsExactly(0, 1);
		assertThat(meeting.getDecisions()).extracting(MeetingDecision::getSource)
			.containsOnly(MeetingItemSource.MANUAL);
		assertThat(meeting.getActionItems()).singleElement().satisfies(item -> {
			assertThat(item.getStatus()).isEqualTo(ActionItemStatus.OPEN);
			assertThat(item.getSource()).isEqualTo(MeetingItemSource.MANUAL);
			assertThat(item.getTaskId()).isNull();
			assertThat(item.getApprovedBy()).isNull();
		});

		meeting.replaceAttendees(List.of());
		meeting.replaceDecisions(List.of("교체 결정"));
		meeting.replaceActionItems(List.of());
		assertThat(meeting.getAttendees()).isEmpty();
		assertThat(meeting.getDecisions()).extracting(MeetingDecision::getContent).containsExactly("교체 결정");
		assertThat(meeting.getActionItems()).isEmpty();
	}

	@Test
	void changesOnlyEditableDetailsAndKeepsOwnership() {
		final Meeting meeting = Meeting.create(10L, "기존", Instant.parse("2026-11-24T01:00:00Z"), null, 20L);

		meeting.changeDetails("변경", Instant.parse("2026-11-25T02:00:00Z"), "새 본문");

		assertThat(meeting.getProjectId()).isEqualTo(10L);
		assertThat(meeting.getAuthorUserId()).isEqualTo(20L);
		assertThat(meeting.getTitle()).isEqualTo("변경");
		assertThat(meeting.getHeldAt()).isEqualTo(Instant.parse("2026-11-25T02:00:00Z"));
		assertThat(meeting.getContent()).isEqualTo("새 본문");
	}
}
