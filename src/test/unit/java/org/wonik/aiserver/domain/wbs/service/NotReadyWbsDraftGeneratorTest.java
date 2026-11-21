package org.wonik.aiserver.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class NotReadyWbsDraftGeneratorTest {

	@Test
	@DisplayName("Java 이식 전에는 항상 503 AI_NOT_READY")
	void notReady() {
		assertThatThrownBy(() -> new NotReadyWbsDraftGenerator().generate(null))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_NOT_READY);
	}
}
