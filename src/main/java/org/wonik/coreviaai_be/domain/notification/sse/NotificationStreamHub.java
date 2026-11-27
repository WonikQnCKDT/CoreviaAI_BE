package org.wonik.coreviaai_be.domain.notification.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로세스 안 SSE 연결. 이 인스턴스에 붙어 있는 수신자에게만 바로 보낸다.
 * 여러 서버로 나뉘면 다른 인스턴스 연결에는 닿지 않고, 목록 조회로 따라잡는다.
 */
@Component
@Slf4j
public class NotificationStreamHub {

	private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;

	private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(final Long userId) {
		final SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
		emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
		emitter.onCompletion(() -> remove(userId, emitter));
		emitter.onTimeout(() -> remove(userId, emitter));
		emitter.onError(error -> remove(userId, emitter));
		try {
			emitter.send(SseEmitter.event().name("ready").data("ok"));
		} catch (final IOException exception) {
			remove(userId, emitter);
		}
		return emitter;
	}

	public void publish(final Long userId, final Object payload) {
		final List<SseEmitter> connected = emitters.get(userId);
		if (connected == null || connected.isEmpty()) {
			return;
		}
		for (final SseEmitter emitter : connected) {
			try {
				emitter.send(SseEmitter.event().name("notification").data(payload));
			} catch (final IOException | IllegalStateException exception) {
				remove(userId, emitter);
				log.debug("알림 SSE 전송에 실패해 연결을 끊었습니다. userId={}", userId);
			}
		}
	}

	private void remove(final Long userId, final SseEmitter emitter) {
		final CopyOnWriteArrayList<SseEmitter> connected = emitters.get(userId);
		if (connected == null) {
			return;
		}
		connected.remove(emitter);
		if (connected.isEmpty()) {
			emitters.remove(userId, connected);
		}
	}
}
