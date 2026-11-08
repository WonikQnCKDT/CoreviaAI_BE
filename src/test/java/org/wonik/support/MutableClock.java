package org.wonik.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 테스트에서 시간을 고정하고 원하는 만큼 앞으로 보내기 위한 Clock. 실제 시간에 의존하지 않게 한다.
 */
public class MutableClock extends Clock {

	private volatile Instant now;

	public MutableClock(final Instant start) {
		this.now = start;
	}

	public void setInstant(final Instant instant) {
		this.now = instant;
	}

	public void advance(final Duration duration) {
		this.now = now.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(final ZoneId zone) {
		return this;
	}

	@Override
	public Instant instant() {
		return now;
	}
}
