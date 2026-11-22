package org.wonik.global.common.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 업무 날짜(DATE 컬럼: 프로젝트 기간, 작업 기간, 마일스톤 목표일)를 판정하는 기준 시간대.
 * <p>
 * 서버 Clock은 UTC(ClockConfig)이고 시점(timestamp)은 UTC로 저장하지만, 업무 날짜는 회사 달력 기준의 날짜다.
 * 본사(경북 구미)와 MVP 사용 조직이 한국에 있으므로 한국 표준시(Asia/Seoul)의 '오늘'을 쓴다.
 * 해외 거점 시간대 기준 마감 표시는 명세서 NFR-19에서 후속(C) 범위다. 서버·JVM 기본 시간대에는 의존하지 않는다.
 */
public final class BusinessCalendar {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

	private BusinessCalendar() {
	}

	/** 주입된 Clock의 현재 시점을 업무 시간대 날짜로 바꾼다. Clock 자체의 시간대는 보지 않는다. */
	public static LocalDate today(final Clock clock) {
		return LocalDate.ofInstant(clock.instant(), ZONE);
	}
}
