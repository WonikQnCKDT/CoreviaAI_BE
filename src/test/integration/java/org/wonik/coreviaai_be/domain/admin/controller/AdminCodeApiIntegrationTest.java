package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.code.entity.CodeGroup;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.code.repository.CodeGroupRepository;
import org.wonik.coreviaai_be.domain.code.repository.CommonCodeRepository;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-07 공통 코드 관리 API를 HTTP 경계에서 검증한다.
 * 시스템 관리자만 호출, 코드 추가·정렬·사용 여부 변경, 코드 값 수정·삭제 불가, 감사 로그(변경 이력).
 * 변경 테스트는 시드 그룹을 건드리지 않도록 TEST_ 그룹을 만들어 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCodeApiIntegrationTest extends AdminApiTestSupport {

	private static final String GROUPS = "/api/admin/code-groups";
	private static final String CODES = GROUPS + "/{groupCode}/codes";
	private static final String CODE = CODES + "/{code}";
	private static final String TEST_GROUP = "TEST_GROUP";

	@Autowired
	private CodeGroupRepository codeGroupRepository;
	@Autowired
	private CommonCodeRepository commonCodeRepository;

	@Test
	@DisplayName("일반 사용자·경영진이 코드 관리 API를 호출하면 404, 로그인하지 않으면 401")
	void onlySystemAdmin() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		final Department team = saveDepartment("개발팀", null);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, team.getId());

		for (final Cookie cookie : List.of(loginOk("user01", USER_PASSWORD), loginOk("exec01", USER_PASSWORD))) {
			expectError(getAs(cookie, GROUPS), ErrorCode.NOT_FOUND);
			expectError(getAs(cookie, CODES, "RISK_LEVEL"), ErrorCode.NOT_FOUND);
			expectError(postJson(GROUPS + "/" + TEST_GROUP + "/codes", cookie, body("code", "A")), ErrorCode.NOT_FOUND);
			expectError(
				patchJson(cookie, body("sortOrder", 1, "active", false), CODE, "RISK_LEVEL", "NORMAL"),
				ErrorCode.NOT_FOUND
			);
		}
		expectError(getAs(null, GROUPS), ErrorCode.UNAUTHORIZED);
		assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId(TEST_GROUP))).isEmpty();
		assertThat(findCode("RISK_LEVEL", "NORMAL").isActive()).isTrue();
	}

	@Test
	@DisplayName("그룹 목록에 시드 4개 그룹이, 그룹별 코드 목록에 정렬 순서대로 코드와 라벨 키가 나온다")
	void listGroupsAndCodes() throws Exception {
		getAs(adminCookie, GROUPS)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(4))
			.andExpect(jsonPath("$.response[0].groupCode").value("RISK_LEVEL"))
			.andExpect(jsonPath("$.response[0].labelKey").value("codeGroup.RISK_LEVEL"))
			.andExpect(jsonPath("$.response[3].groupCode").value("PROJECT_STATUS"));

		getAs(adminCookie, CODES, "RISK_LEVEL")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(3))
			.andExpect(jsonPath("$.response[0].code").value("NORMAL"))
			.andExpect(jsonPath("$.response[0].groupCode").value("RISK_LEVEL"))
			.andExpect(jsonPath("$.response[0].labelKey").value("code.RISK_LEVEL.NORMAL"))
			.andExpect(jsonPath("$.response[0].active").value(true))
			.andExpect(jsonPath("$.response[1].code").value("CAUTION"))
			.andExpect(jsonPath("$.response[2].code").value("DANGER"));

		expectError(getAs(adminCookie, CODES, "NO_SUCH_GROUP"), ErrorCode.CODE_GROUP_NOT_FOUND);
		getAs(adminCookie, CODES, "risk-level")
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("코드를 추가하면 라벨 키는 규칙대로 정해지고, 순서를 주지 않으면 마지막 뒤에 붙으며, 생성 이력이 남는다")
	void createCode() throws Exception {
		saveTestCodeGroup(TEST_GROUP);

		final JsonNode first = readResponse(postJson(codesUrl(), adminCookie, body("code", "ALPHA", "sortOrder", 5)));
		assertThat(first.get("code").asString()).isEqualTo("ALPHA");
		assertThat(first.get("labelKey").asString()).isEqualTo("code.TEST_GROUP.ALPHA");
		assertThat(first.get("sortOrder").asInt()).isEqualTo(5);
		assertThat(first.get("active").asBoolean()).isTrue();

		final JsonNode second = readResponse(postJson(codesUrl(), adminCookie, body("code", "BETA", "sortOrder", null)));
		assertThat(second.get("sortOrder").asInt()).isEqualTo(6);

		final List<AuditLog> logs = auditLogs(AuditAction.CODE_CREATED);
		assertThat(logs).hasSize(2).allSatisfy(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.CODE);
			assertThat(log.getActorUserId()).isEqualTo(adminId);
		});
		final JsonNode details = jsonMapper.readTree(logs.get(0).getDetails());
		assertThat(details.get("groupCode").asString()).isEqualTo(TEST_GROUP);
		assertThat(details.get("code").asString()).isEqualTo("ALPHA");
		assertThat(details.get("labelKey").asString()).isEqualTo("code.TEST_GROUP.ALPHA");
	}

	@Test
	@DisplayName("빈 그룹의 첫 코드는 순서 1이고, 순서 경계값 0·9999는 허용한다")
	void createCodeSortOrderBoundaries() throws Exception {
		saveTestCodeGroup(TEST_GROUP);

		assertThat(readResponse(postJson(codesUrl(), adminCookie, body("code", "FIRST"))).get("sortOrder").asInt()).isEqualTo(1);
		assertThat(readResponse(postJson(codesUrl(), adminCookie, body("code", "ZERO", "sortOrder", 0)))
			.get("sortOrder").asInt()).isZero();
		assertThat(readResponse(postJson(codesUrl(), adminCookie, body("code", "MAX", "sortOrder", 9999)))
			.get("sortOrder").asInt()).isEqualTo(9999);
	}

	@Test
	@DisplayName("코드 추가 입력 오류: 형식 위반·순서 범위 밖은 400, 없는 그룹은 404, 같은 코드 재등록은 409")
	void createCodeRejected() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		postJson(codesUrl(), adminCookie, body("code", "ALPHA")).andExpect(status().isOk());

		for (final Object badCode : new Object[] {null, "", "alpha", "1ABC", "A-B", "A".repeat(41)}) {
			postJson(codesUrl(), adminCookie, body("code", badCode))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false));
		}
		postJson(codesUrl(), adminCookie, body("code", "A".repeat(40))).andExpect(status().isOk());
		postJson(codesUrl(), adminCookie, body("code", "NEG", "sortOrder", -1)).andExpect(status().isBadRequest());
		postJson(codesUrl(), adminCookie, body("code", "BIG", "sortOrder", 10000)).andExpect(status().isBadRequest());

		expectError(postJson(GROUPS + "/TEST_NONE/codes", adminCookie, body("code", "ALPHA")), ErrorCode.CODE_GROUP_NOT_FOUND);
		expectError(postJson(codesUrl(), adminCookie, body("code", "ALPHA")), ErrorCode.DUPLICATE_CODE);
		assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId(TEST_GROUP))).hasSize(2);
		assertThat(auditLogs(AuditAction.CODE_CREATED)).hasSize(2);
	}

	@Test
	@DisplayName("정렬 순서·사용 여부를 바꾸면 변경 전·후가 이력에 남고, 본문에 넣은 코드 값·라벨 키는 바뀌지 않는다")
	void updateCode() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		postJson(codesUrl(), adminCookie, body("code", "ALPHA", "sortOrder", 1)).andExpect(status().isOk());

		final JsonNode updated = readResponse(patchJson(
			adminCookie,
			body("sortOrder", 3, "active", false, "code", "HACKED", "labelKey", "code.OTHER.X"),
			CODE, TEST_GROUP, "ALPHA"
		));
		assertThat(updated.get("code").asString()).isEqualTo("ALPHA");
		assertThat(updated.get("labelKey").asString()).isEqualTo("code.TEST_GROUP.ALPHA");
		assertThat(updated.get("sortOrder").asInt()).isEqualTo(3);
		assertThat(updated.get("active").asBoolean()).isFalse();

		final CommonCode stored = findCode(TEST_GROUP, "ALPHA");
		assertThat(stored.getLabelKey()).isEqualTo("code.TEST_GROUP.ALPHA");
		assertThat(stored.isActive()).isFalse();

		final List<AuditLog> logs = auditLogs(AuditAction.CODE_CHANGED);
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getTargetId()).isEqualTo(stored.getId());
		final JsonNode details = jsonMapper.readTree(logs.get(0).getDetails());
		assertThat(details.get("code").asString()).isEqualTo("ALPHA");
		assertThat(details.get("sortOrder").get("before").asInt()).isEqualTo(1);
		assertThat(details.get("sortOrder").get("after").asInt()).isEqualTo(3);
		assertThat(details.get("active").get("before").asBoolean()).isTrue();
		assertThat(details.get("active").get("after").asBoolean()).isFalse();

		// 같은 값 재전송은 변경·이력 없음
		patchJson(adminCookie, body("sortOrder", 3, "active", false), CODE, TEST_GROUP, "ALPHA").andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.CODE_CHANGED)).hasSize(1);

		// 사용 여부만 다시 켜면 바뀐 필드만 기록
		patchJson(adminCookie, body("sortOrder", 3, "active", true), CODE, TEST_GROUP, "ALPHA").andExpect(status().isOk());
		final JsonNode second = jsonMapper.readTree(auditLogs(AuditAction.CODE_CHANGED).get(1).getDetails());
		assertThat(second.has("sortOrder")).isFalse();
		assertThat(second.get("active").get("after").asBoolean()).isTrue();
	}

	@Test
	@DisplayName("변경 입력 오류: 필수 누락·순서 범위 밖은 400, 없는 코드·그룹은 404")
	void updateCodeRejected() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		postJson(codesUrl(), adminCookie, body("code", "ALPHA")).andExpect(status().isOk());

		patchJson(adminCookie, body("sortOrder", null, "active", true), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("sortOrder", 1, "active", null), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("sortOrder", -1, "active", true), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("sortOrder", 10000, "active", true), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		expectError(
			patchJson(adminCookie, body("sortOrder", 1, "active", true), CODE, TEST_GROUP, "NOPE"),
			ErrorCode.CODE_NOT_FOUND
		);
		expectError(
			patchJson(adminCookie, body("sortOrder", 1, "active", true), CODE, "TEST_NONE", "ALPHA"),
			ErrorCode.CODE_GROUP_NOT_FOUND
		);
		assertThat(auditLogs(AuditAction.CODE_CHANGED)).isEmpty();
	}

	@Test
	@DisplayName("코드 값 수정(PUT)·삭제(DELETE) API는 없어 404이고 코드는 그대로 남는다")
	void codeValueCannotBeChangedOrDeleted() throws Exception {
		expectError(deleteAs(adminCookie, CODE, "RISK_LEVEL", "NORMAL"), ErrorCode.NOT_FOUND);
		expectError(
			putJson(adminCookie, body("code", "OK", "sortOrder", 1, "active", true), CODE, "RISK_LEVEL", "NORMAL"),
			ErrorCode.NOT_FOUND
		);
		// 그룹 단건 경로는 어떤 메서드로도 매핑이 없어 정적 리소스 404(본문 없음)로 끝난다
		deleteAs(adminCookie, GROUPS + "/{groupCode}", "RISK_LEVEL").andExpect(status().isNotFound());

		assertThat(findCode("RISK_LEVEL", "NORMAL").getLabelKey()).isEqualTo("code.RISK_LEVEL.NORMAL");
		assertThat(codeGroupRepository.findByGroupCode("RISK_LEVEL")).isPresent();
	}

	private String codesUrl() {
		return GROUPS + "/" + TEST_GROUP + "/codes";
	}

	private Long groupId(final String groupCode) {
		return codeGroupRepository.findByGroupCode(groupCode).map(CodeGroup::getId).orElseThrow();
	}

	private CommonCode findCode(final String groupCode, final String code) {
		return commonCodeRepository.findByGroupIdAndCode(groupId(groupCode), code).orElseThrow();
	}
}
