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
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-07 공통 코드 관리 API를 HTTP 경계에서 검증한다.
 * 시스템 관리자만 호출, 정렬 순서 변경, 코드 추가·수정·삭제와 사용 여부 변경 불가(코드 값 고정), 감사 로그(변경 이력).
 * 변경 테스트는 시드 그룹을 건드리지 않도록 TEST_ 그룹과 코드를 SQL로 만들어 쓴다.
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
	@Autowired
	private I18nLabelRepository i18nLabelRepository;

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
				patchJson(cookie, body("sortOrder", 9), CODE, "RISK_LEVEL", "NORMAL"),
				ErrorCode.NOT_FOUND
			);
		}
		expectError(getAs(null, GROUPS), ErrorCode.UNAUTHORIZED);
		assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId(TEST_GROUP))).isEmpty();
		assertThat(findCode("RISK_LEVEL", "NORMAL").getSortOrder()).isEqualTo(1);
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
	@DisplayName("코드 추가 API는 없어 404이고 코드도 이력도 생기지 않는다(FR-ADM-07: 코드 값은 고정)")
	void codeCannotBeAdded() throws Exception {
		saveTestCodeGroup(TEST_GROUP);

		expectError(postJson(codesUrl(), adminCookie, body("code", "ALPHA", "sortOrder", 1)), ErrorCode.NOT_FOUND);
		expectError(postJson(GROUPS + "/RISK_LEVEL/codes", adminCookie, body("code", "EXTRA")), ErrorCode.NOT_FOUND);

		assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId(TEST_GROUP))).isEmpty();
		assertThat(commonCodeRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId("RISK_LEVEL"))).hasSize(3);
		assertThat(auditLogRepository.findAll()).noneMatch(log -> log.getTargetType() == AuditTargetType.CODE);
	}

	@Test
	@DisplayName("정렬 순서를 바꾸면 변경 전·후가 이력에 남고, 본문에 넣은 코드 값·라벨 키·사용 여부는 무시된다")
	void changeSortOrder() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		saveTestCode(TEST_GROUP, "ALPHA", 1, true);

		final JsonNode updated = readResponse(patchJson(
			adminCookie,
			body("sortOrder", 3, "active", false, "code", "HACKED", "labelKey", "code.OTHER.X"),
			CODE, TEST_GROUP, "ALPHA"
		));
		assertThat(updated.get("code").asString()).isEqualTo("ALPHA");
		assertThat(updated.get("labelKey").asString()).isEqualTo("code.TEST_GROUP.ALPHA");
		assertThat(updated.get("sortOrder").asInt()).isEqualTo(3);
		assertThat(updated.get("active").asBoolean()).isTrue();

		final CommonCode stored = findCode(TEST_GROUP, "ALPHA");
		assertThat(stored.getSortOrder()).isEqualTo(3);
		assertThat(stored.getLabelKey()).isEqualTo("code.TEST_GROUP.ALPHA");
		assertThat(stored.isActive()).isTrue();

		final List<AuditLog> logs = auditLogs(AuditAction.CODE_CHANGED);
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getTargetType()).isEqualTo(AuditTargetType.CODE);
		assertThat(logs.get(0).getTargetId()).isEqualTo(stored.getId());
		assertThat(logs.get(0).getActorUserId()).isEqualTo(adminId);
		final JsonNode details = jsonMapper.readTree(logs.get(0).getDetails());
		assertThat(details.get("groupCode").asString()).isEqualTo(TEST_GROUP);
		assertThat(details.get("code").asString()).isEqualTo("ALPHA");
		assertThat(details.get("sortOrder").get("before").asInt()).isEqualTo(1);
		assertThat(details.get("sortOrder").get("after").asInt()).isEqualTo(3);
		assertThat(details.has("active")).isFalse();

		// 같은 순서 재전송은 변경·이력 없음(사용 여부만 다르게 보내도 무시되므로 변경 없음)
		patchJson(adminCookie, body("sortOrder", 3), CODE, TEST_GROUP, "ALPHA").andExpect(status().isOk());
		patchJson(adminCookie, body("sortOrder", 3, "active", false), CODE, TEST_GROUP, "ALPHA").andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.CODE_CHANGED)).hasSize(1);
		assertThat(findCode(TEST_GROUP, "ALPHA").isActive()).isTrue();
	}

	@Test
	@DisplayName("마이그레이션으로 사용 안 함이 된 코드도 정렬 순서는 바꿀 수 있고 사용 여부는 그대로다")
	void inactiveCodeKeepsInactive() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		saveTestCode(TEST_GROUP, "OLD", 5, false);

		final JsonNode updated = readResponse(patchJson(adminCookie, body("sortOrder", 2, "active", true), CODE, TEST_GROUP, "OLD"));
		assertThat(updated.get("sortOrder").asInt()).isEqualTo(2);
		assertThat(updated.get("active").asBoolean()).isFalse();
		assertThat(findCode(TEST_GROUP, "OLD").isActive()).isFalse();
	}

	@Test
	@DisplayName("정렬 순서 경계값 0·9999는 허용한다")
	void sortOrderBoundaries() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		saveTestCode(TEST_GROUP, "ALPHA", 1, true);

		assertThat(readResponse(patchJson(adminCookie, body("sortOrder", 0), CODE, TEST_GROUP, "ALPHA")).get("sortOrder").asInt())
			.isZero();
		assertThat(readResponse(patchJson(adminCookie, body("sortOrder", 9999), CODE, TEST_GROUP, "ALPHA")).get("sortOrder").asInt())
			.isEqualTo(9999);
		assertThat(auditLogs(AuditAction.CODE_CHANGED)).hasSize(2);
	}

	@Test
	@DisplayName("변경 입력 오류: 순서 누락·범위 밖(-1, 10000)은 400, 없는 코드·그룹은 404")
	void changeSortOrderRejected() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		saveTestCode(TEST_GROUP, "ALPHA", 1, true);

		patchJson(adminCookie, body("sortOrder", null), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("active", false), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("sortOrder", -1), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		patchJson(adminCookie, body("sortOrder", 10000), CODE, TEST_GROUP, "ALPHA").andExpect(status().isBadRequest());
		expectError(patchJson(adminCookie, body("sortOrder", 1), CODE, TEST_GROUP, "NOPE"), ErrorCode.CODE_NOT_FOUND);
		expectError(patchJson(adminCookie, body("sortOrder", 1), CODE, "TEST_NONE", "ALPHA"), ErrorCode.CODE_GROUP_NOT_FOUND);
		assertThat(auditLogs(AuditAction.CODE_CHANGED)).isEmpty();
		assertThat(findCode(TEST_GROUP, "ALPHA").isActive()).isTrue();
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

	@Test
	@DisplayName("코드를 추가하면 라벨이 없을 때 한국어 = 코드 값인 라벨이 함께 생기고(번역은 누락), 이미 있으면 그대로 둔다")
	void createCodeCreatesMissingLabel() throws Exception {
		saveTestCodeGroup(TEST_GROUP);
		i18nLabelRepository.save(I18nLabel.create("code.TEST_GROUP.PRESET", "미리 등록", "Preset", null));

		postJson(codesUrl(), adminCookie, body("code", "ALPHA")).andExpect(status().isOk());
		postJson(codesUrl(), adminCookie, body("code", "PRESET")).andExpect(status().isOk());

		final I18nLabel created = i18nLabelRepository.findByLabelKey("code.TEST_GROUP.ALPHA").orElseThrow();
		assertThat(created.getTextKo()).isEqualTo("ALPHA");
		assertThat(created.getTextEn()).isNull();
		assertThat(created.getTextZhTw()).isNull();
		final I18nLabel preset = i18nLabelRepository.findByLabelKey("code.TEST_GROUP.PRESET").orElseThrow();
		assertThat(preset.getTextKo()).isEqualTo("미리 등록");
		assertThat(preset.getVersion()).isZero();
		assertThat(auditLogs(AuditAction.I18N_LABEL_CREATED)).singleElement()
			.satisfies(log -> assertThat(log.getTargetId()).isEqualTo(created.getId()));

		// 코드 추가가 실패(중복)하면 라벨도 생기지 않는다(같은 트랜잭션)
		expectError(postJson(codesUrl(), adminCookie, body("code", "ALPHA")), ErrorCode.DUPLICATE_CODE);
		assertThat(auditLogs(AuditAction.I18N_LABEL_CREATED)).hasSize(1);
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
