package org.wonik.coreviaai_be.domain.meeting.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.wonik.coreviaai_be.domain.meeting.service.command.SaveMeetingCommand;

/** 길이 접두사와 null 마커로 서로 다른 입력이 같은 바이트열이 되지 않게 한다. */
public final class MeetingRequestHash {
	private MeetingRequestHash() {
	}

	public static String of(final SaveMeetingCommand command) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			add(digest, command.title().strip());
			add(digest, command.heldAt().toString());
			add(digest, command.content());
			add(digest, Integer.toString(command.attendees().size()));
			command.attendees().forEach(a -> {
				add(digest, a.userId() == null ? null : a.userId().toString());
				add(digest, a.externalName() == null ? null : a.externalName().strip());
			});
			add(digest, Integer.toString(command.decisions().size()));
			command.decisions().forEach(value -> add(digest, value.strip()));
			add(digest, Integer.toString(command.actionItems().size()));
			command.actionItems().forEach(item -> {
				add(digest, item.content().strip());
				add(digest, item.assigneeUserId() == null ? null : item.assigneeUserId().toString());
				add(digest, item.dueDate() == null ? null : item.dueDate().toString());
			});
			return HexFormat.of().formatHex(digest.digest());
		} catch (final NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("Java runtime must support SHA-256", impossible);
		}
	}

	private static void add(final MessageDigest digest, final String value) {
		if (value == null) {
			digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
			return;
		}
		final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
		digest.update(bytes);
	}
}
