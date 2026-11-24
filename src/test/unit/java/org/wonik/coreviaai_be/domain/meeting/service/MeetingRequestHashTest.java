package org.wonik.coreviaai_be.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.meeting.service.command.SaveMeetingCommand;

class MeetingRequestHashTest {
	@Test
	void distinguishesNullEmptyAndFieldBoundariesWhileNormalizingTitle() {
		final Instant heldAt = Instant.parse("2026-11-24T00:00:00Z");
		final var original = new SaveMeetingCommand("회의", heldAt, null, List.of(), List.of("ab", "c"), List.of());
		final var normalized = new SaveMeetingCommand(" 회의 ", heldAt, null, List.of(), List.of("ab", "c"), List.of());
		final var different = new SaveMeetingCommand("회의", heldAt, null, List.of(), List.of("a", "bc"), List.of());
		final var empty = new SaveMeetingCommand("회의", heldAt, "", List.of(), List.of("ab", "c"), List.of());
		assertThat(MeetingRequestHash.of(original)).isEqualTo(MeetingRequestHash.of(normalized));
		assertThat(MeetingRequestHash.of(original)).isNotEqualTo(MeetingRequestHash.of(different));
		assertThat(MeetingRequestHash.of(original)).isNotEqualTo(MeetingRequestHash.of(empty));
	}
}
