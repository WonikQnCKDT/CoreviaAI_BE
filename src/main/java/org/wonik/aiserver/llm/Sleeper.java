package org.wonik.aiserver.llm;

import java.time.Duration;

/**
 * 재시도 전에 잠깐 쉬는 동작.
 * 테스트에서는 적용하지 않음 
 */
@FunctionalInterface
public interface Sleeper {

	/** 실제로 쉬는 기본 구현. */
	Sleeper SYSTEM = duration -> Thread.sleep(duration.toMillis());

	void sleep(Duration duration) throws InterruptedException;
}
