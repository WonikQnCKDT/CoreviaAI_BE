package org.wonik.support;

import java.io.IOException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 에러 로그(FR-ADM-09) 통합 테스트 전용: 미처리 예외·업무 예외를 일부러 일으킨다. 테스트 소스에만 있어 운영에는 없다.
 * /api/** 보안 체인 아래라 로그인한 사용자만 호출할 수 있다. 메시지의 비밀값·개인정보는 모두 가짜다.
 */
@RestController
@RequestMapping(ErrorTriggerTestController.PATH)
@RequiredArgsConstructor
public class ErrorTriggerTestController {

	public static final String PATH = "/api/test-errors";
	public static final String SECRET_MESSAGE = "db login failed password=Secr3t! contact hong@example.com";
	public static final String CAUSE_MESSAGE = "Key (login_id)=(hong) already exists.";
	public static final String ROLLBACK_GROUP_CODE = "TEST_ERRLOG_ROLLBACK";

	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;

	@GetMapping("/unhandled")
	public void unhandled() {
		throw new IllegalStateException(SECRET_MESSAGE, new RuntimeException(CAUSE_MESSAGE));
	}

	@GetMapping("/checked")
	public void checked() throws IOException {
		throw new IOException("disk full token=abc123");
	}

	@GetMapping("/business")
	public void business() {
		throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
	}

	/** 업무 트랜잭션 안에서 데이터를 바꾼 뒤 실패한다. 업무 변경은 롤백되고 에러 로그는 남아야 한다. */
	@PostMapping("/rollback")
	public void rollback() {
		transactionTemplate.executeWithoutResult(status -> {
			jdbcTemplate.update(
				"INSERT INTO code_groups (group_code, label_key, created_at, updated_at)"
					+ " VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
				ROLLBACK_GROUP_CODE, "codeGroup." + ROLLBACK_GROUP_CODE
			);
			throw new IllegalStateException("업무 처리 중 실패");
		});
	}
}
