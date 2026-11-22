package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.errorlog.ErrorLogRecorder;
import org.wonik.global.errorlog.ErrorLogRepository;
import org.wonik.support.ApiIntegrationTestSupport;
import org.wonik.support.ErrorTriggerTestController;

/**
 * 에러 로그 저장이 실패해도(DB 장애 등) 사용자 응답은 원래의 공통 500 그대로이고 새 예외가 응답에 섞이지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorLogRecordFailureTest extends ApiIntegrationTestSupport {

	@MockitoSpyBean
	private ErrorLogRecorder errorLogRecorder;
	@Autowired
	private ErrorLogRepository errorLogRepository;

	@Test
	@DisplayName("에러 로그 기록이 실패해도 응답은 같은 500 공통 오류다")
	void responseUnchangedWhenRecordingFails() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, saveDepartment("개발팀", null).getId());
		doThrow(new IllegalStateException("error log store unavailable"))
			.when(errorLogRecorder).record(any(), any(), any(), any());

		final ResultActions actions = getAs(loginOk("user01", USER_PASSWORD), ErrorTriggerTestController.PATH + "/unhandled");

		expectError(actions, ErrorCode.INTERNAL_SERVER_ERROR);
		assertThat(actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
			.doesNotContain("unavailable", "Secr3t");
		verify(errorLogRecorder).record(any(), any(), any(), any());
		assertThat(errorLogRepository.count()).isZero();
	}
}
