package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
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
 * FR-ADM-06 다국어 라벨 관리 API를 HTTP 경계에서 검증한다.
 * 시스템 관리자만 호출, 라벨 등록·문구 수정(낙관적 잠금), 키·누락 필터 목록(커서), 문구 변경 이력(감사 로그), 키 변경·삭제 불가.
 * 시드 라벨은 바꾸지 않도록 test. 키를 만들어 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminLabelApiIntegrationTest extends AdminApiTestSupport {

	private static final String LABELS = "/api/admin/i18n/labels";
	private static final String LABEL = LABELS + "/{labelId}";

	@Autowired
	private I18nLabelRepository i18nLabelRepository;

	@Test
	@DisplayName("일반 사용자·경영진이 라벨 관리 API를 호출하면 404, 로그인하지 않으면 401이고 아무것도 바뀌지 않는다")
	void onlySystemAdmin() throws Exception {
		final I18nLabel label = saveLabel("test.menu.home", "홈", null, null);
		final Department team = saveDepartment("개발팀", null);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, team.getId());

		for (final Cookie cookie : List.of(loginOk("user01", USER_PASSWORD), loginOk("exec01", USER_PASSWORD))) {
			expectError(getAs(cookie, LABELS), ErrorCode.NOT_FOUND);
			expectError(postJson(LABELS, cookie, body("labelKey", "test.x", "textKo", "엑스")), ErrorCode.NOT_FOUND);
			expectError(patchJson(cookie, body("textKo", "바꿈", "version", 0), LABEL, label.getId()), ErrorCode.NOT_FOUND);
		}
		expectError(getAs(null, LABELS), ErrorCode.UNAUTHORIZED);
		expectError(postJson(LABELS, null, body("labelKey", "test.x", "textKo", "엑스")), ErrorCode.UNAUTHORIZED);

		assertThat(i18nLabelRepository.existsByLabelKey("test.x")).isFalse();
		assertThat(i18nLabelRepository.findById(label.getId()).orElseThrow().getTextKo()).isEqualTo("홈");
	}

	@Test
	@DisplayName("라벨을 등록하면 문구는 앞뒤 공백을 지우고 빈 번역은 누락(null)으로 저장되며, 등록 이력이 남는다")
	void createLabel() throws Exception {
		final JsonNode created = readResponse(postJson(LABELS, adminCookie,
			body("labelKey", "test.button.save", "textKo", " 저장 ", "textEn", "Save", "textZhTw", "  ")));
		assertThat(created.get("labelKey").asString()).isEqualTo("test.button.save");
		assertThat(created.get("textKo").asString()).isEqualTo("저장");
		assertThat(created.get("textEn").asString()).isEqualTo("Save");
		assertThat(created.get("textZhTw").isNull()).isTrue();
		assertThat(created.get("version").asLong()).isZero();

		final List<AuditLog> logs = auditLogs(AuditAction.I18N_LABEL_CREATED);
		assertThat(logs).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.I18N_LABEL);
			assertThat(log.getTargetId()).isEqualTo(created.get("id").asLong());
			assertThat(log.getActorUserId()).isEqualTo(adminId);
		});
		final JsonNode details = jsonMapper.readTree(logs.get(0).getDetails());
		assertThat(details.get("labelKey").asString()).isEqualTo("test.button.save");
		assertThat(details.get("textKo").asString()).isEqualTo("저장");
		assertThat(details.get("textZhTw").isNull()).isTrue();
	}

	@Test
	@DisplayName("라벨 등록 입력 오류는 400, 같은 키 재등록은 409이고 행이 하나만 남는다")
	void createLabelRejected() throws Exception {
		postJson(LABELS, adminCookie, body("labelKey", "test.dup", "textKo", "중복")).andExpect(status().isOk());
		expectError(postJson(LABELS, adminCookie, body("labelKey", "test.dup", "textKo", "다시")), ErrorCode.DUPLICATE_LABEL_KEY);
		// 시드 라벨 키도 중복이다.
		expectError(postJson(LABELS, adminCookie, body("labelKey", "code.RISK_LEVEL.NORMAL", "textKo", "정상")),
			ErrorCode.DUPLICATE_LABEL_KEY);

		for (final Object badKey : new Object[] {null, "", "1test", "test..x", "test.x.", "테스트.키", "test x", "t".repeat(101)}) {
			postJson(LABELS, adminCookie, body("labelKey", badKey, "textKo", "문구"))
				.andExpect(status().isBadRequest());
		}
		for (final Object badKo : new Object[] {null, "", "   ", "가".repeat(501), "줄\n바꿈"}) {
			postJson(LABELS, adminCookie, body("labelKey", "test.bad", "textKo", badKo))
				.andExpect(status().isBadRequest());
		}
		postJson(LABELS, adminCookie, body("labelKey", "test.bad", "textKo", "문구", "textEn", "e".repeat(501)))
			.andExpect(status().isBadRequest());
		postJson(LABELS, adminCookie, body("labelKey", "test.bad", "textKo", "문구", "textZhTw", "탭\t"))
			.andExpect(status().isBadRequest());

		assertThat(i18nLabelRepository.existsByLabelKey("test.bad")).isFalse();
		assertThat(auditLogs(AuditAction.I18N_LABEL_CREATED)).hasSize(1);
	}

	@Test
	@DisplayName("경계값: 라벨 키 100자와 문구 500자는 등록된다")
	void createLabelBoundaries() throws Exception {
		final String key = "test." + "k".repeat(95);
		final JsonNode created = readResponse(postJson(LABELS, adminCookie,
			body("labelKey", key, "textKo", "가".repeat(500), "textEn", "e".repeat(500), "textZhTw", "中".repeat(500))));
		assertThat(created.get("labelKey").asString()).hasSize(100);
		assertThat(created.get("textKo").asString()).hasSize(500);
		// 세 언어 500자 등록 이력도 감사 로그 details 최대 길이 안에 들어간다.
		assertThat(auditLogs(AuditAction.I18N_LABEL_CREATED)).hasSize(1);
	}

	@Test
	@DisplayName("문구를 수정하면 version이 오르고, 바뀐 언어마다 전·후 문구 이력이 한 행씩 남는다")
	void updateLabel() throws Exception {
		final I18nLabel label = saveLabel("test.menu.home", "홈", "Home", null);

		final JsonNode updated = readResponse(patchJson(adminCookie,
			body("textKo", "홈 화면", "textEn", "Home", "textZhTw", "首頁", "version", label.getVersion()), LABEL, label.getId()));
		assertThat(updated.get("textKo").asString()).isEqualTo("홈 화면");
		assertThat(updated.get("textZhTw").asString()).isEqualTo("首頁");
		assertThat(updated.get("version").asLong()).isEqualTo(label.getVersion() + 1);

		final List<AuditLog> logs = auditLogs(AuditAction.I18N_LABEL_CHANGED);
		assertThat(logs).hasSize(2).allSatisfy(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.I18N_LABEL);
			assertThat(log.getTargetId()).isEqualTo(label.getId());
			assertThat(log.getActorUserId()).isEqualTo(adminId);
		});
		final List<JsonNode> details = new ArrayList<>();
		for (final AuditLog log : logs) {
			details.add(jsonMapper.readTree(log.getDetails()));
		}
		assertThat(details).anySatisfy(d -> {
			assertThat(d.get("labelKey").asString()).isEqualTo("test.menu.home");
			assertThat(d.get("lang").asString()).isEqualTo("ko");
			assertThat(d.get("text").get("before").asString()).isEqualTo("홈");
			assertThat(d.get("text").get("after").asString()).isEqualTo("홈 화면");
		});
		assertThat(details).anySatisfy(d -> {
			assertThat(d.get("lang").asString()).isEqualTo("zh-TW");
			assertThat(d.get("text").get("before").isNull()).isTrue();
			assertThat(d.get("text").get("after").asString()).isEqualTo("首頁");
		});
	}

	@Test
	@DisplayName("번역을 비우면 누락으로 돌아가고, 같은 값 재전송은 version이 달라도 변경·이력 없이 200")
	void updateIdempotentAndClear() throws Exception {
		final I18nLabel label = saveLabel("test.menu.home", "홈", "Home", null);

		final JsonNode cleared = readResponse(patchJson(adminCookie,
			body("textKo", "홈", "textEn", " ", "version", label.getVersion()), LABEL, label.getId()));
		assertThat(cleared.get("textEn").isNull()).isTrue();
		assertThat(auditLogs(AuditAction.I18N_LABEL_CHANGED)).hasSize(1);

		// 성공한 요청의 재전송(옛 version): 현재와 같은 값이라 409가 아니라 현재 상태를 돌려준다.
		final JsonNode again = readResponse(patchJson(adminCookie,
			body("textKo", "홈", "textEn", null, "version", label.getVersion()), LABEL, label.getId()));
		assertThat(again.get("version").asLong()).isEqualTo(cleared.get("version").asLong());
		assertThat(auditLogs(AuditAction.I18N_LABEL_CHANGED)).hasSize(1);
	}

	@Test
	@DisplayName("다른 관리자가 먼저 고친 뒤 옛 version으로 다른 값을 보내면 409이고 문구는 먼저 고친 값으로 남는다")
	void updateStaleVersion() throws Exception {
		final I18nLabel label = saveLabel("test.menu.home", "홈", null, null);
		patchJson(adminCookie, body("textKo", "첫 수정", "version", label.getVersion()), LABEL, label.getId())
			.andExpect(status().isOk());

		expectError(
			patchJson(adminCookie, body("textKo", "두 번째 수정", "version", label.getVersion()), LABEL, label.getId()),
			ErrorCode.CONCURRENT_MODIFICATION
		);
		assertThat(i18nLabelRepository.findById(label.getId()).orElseThrow().getTextKo()).isEqualTo("첫 수정");
		assertThat(auditLogs(AuditAction.I18N_LABEL_CHANGED)).hasSize(1);
	}

	@Test
	@DisplayName("수정 입력 오류는 400, 없는 라벨은 404. 라벨 키 변경·삭제 API는 없다")
	void updateRejected() throws Exception {
		final I18nLabel label = saveLabel("test.menu.home", "홈", null, null);

		patchJson(adminCookie, body("textKo", "", "version", 0), LABEL, label.getId()).andExpect(status().isBadRequest());
		patchJson(adminCookie, body("textKo", "홈"), LABEL, label.getId()).andExpect(status().isBadRequest());
		patchJson(adminCookie, body("textKo", "홈", "version", -1), LABEL, label.getId()).andExpect(status().isBadRequest());
		patchJson(adminCookie, body("textKo", "가".repeat(501), "version", 0), LABEL, label.getId())
			.andExpect(status().isBadRequest());
		patchJson(adminCookie, body("textKo", "홈", "version", 0), LABEL, 0).andExpect(status().isBadRequest());
		expectError(patchJson(adminCookie, body("textKo", "홈", "version", 0), LABEL, 999_999L), ErrorCode.LABEL_NOT_FOUND);

		// 본문에 labelKey를 넣어도 키는 바뀌지 않는다.
		patchJson(adminCookie, body("labelKey", "test.renamed", "textKo", "홈 화면", "version", 0), LABEL, label.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.labelKey").value("test.menu.home"));
		expectError(deleteAs(adminCookie, LABEL, label.getId()), ErrorCode.NOT_FOUND);
		assertThat(i18nLabelRepository.existsByLabelKey("test.menu.home")).isTrue();
	}

	@Test
	@DisplayName("목록은 id 오름차순 커서 페이지이고, 키·한국어 문구로 검색하며 _ 와 % 는 글자 그대로 찾는다")
	void listAndSearch() throws Exception {
		final I18nLabel a = saveLabel("test.list.abx", "알파 버튼", "Alpha", "阿爾法");
		final I18nLabel b = saveLabel("test.list.beta_x", "베타", null, "貝塔");
		final I18nLabel c = saveLabel("test.list.gamma", "감마 100%", "Gamma", null);

		final JsonNode first = readResponse(getAs(adminCookie, LABELS + "?keyword=TEST.LIST&size=2"));
		assertThat(first.get("items").size()).isEqualTo(2);
		assertThat(first.get("items").get(0).get("id").asLong()).isEqualTo(a.getId());
		assertThat(first.get("items").get(1).get("id").asLong()).isEqualTo(b.getId());
		assertThat(first.get("hasNext").asBoolean()).isTrue();

		final JsonNode second = readResponse(getAs(adminCookie,
			LABELS + "?keyword=test.list&size=2&cursor=" + first.get("nextCursor").asString()));
		assertThat(second.get("items").size()).isEqualTo(1);
		assertThat(second.get("items").get(0).get("id").asLong()).isEqualTo(c.getId());
		assertThat(second.get("hasNext").asBoolean()).isFalse();
		assertThat(second.get("nextCursor").isNull()).isTrue();

		// 한국어 문구 검색
		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?keyword=알파")))).containsExactly(a.getId());
		// _ 는 와일드카드가 아니라 글자: beta_x만 찾고 abx(a?x)는 제외
		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?keyword=a_x")))).containsExactly(b.getId());
		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?keyword={keyword}", "100%")))).containsExactly(c.getId());
		// 검색어가 없으면 시드 라벨까지 전체. V9 코드 라벨 18개와 V24 메뉴 라벨 5개.
		assertThat(readResponse(getAs(adminCookie, LABELS + "?size=100")).get("items").size()).isEqualTo(18 + 5 + 3);
	}

	@Test
	@DisplayName("번역 누락 필터: missing=en이면 영어 누락, missing=zh-TW면 번체 중국어 누락 라벨만 준다")
	void missingFilter() throws Exception {
		final I18nLabel a = saveLabel("test.list.alpha", "알파", "Alpha", "阿爾法");
		final I18nLabel b = saveLabel("test.list.beta", "베타", null, "貝塔");
		final I18nLabel c = saveLabel("test.list.gamma", "감마", "Gamma", null);
		final I18nLabel d = saveLabel("test.list.delta", "델타", null, null);

		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?missing=en&size=100"))))
			.containsExactly(b.getId(), d.getId());
		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?missing=zh-TW&keyword=test.&size=100"))))
			.containsExactly(c.getId(), d.getId());
		assertThat(ids(readResponse(getAs(adminCookie, LABELS + "?missing=zh-tw&size=100"))))
			.containsExactly(c.getId(), d.getId())
			.doesNotContain(a.getId());
	}

	@Test
	@DisplayName("목록 요청 오류: 누락 필터에 ko·미지원 언어는 400, size 범위 밖·잘못된 커서는 400")
	void listRejected() throws Exception {
		expectError(getAs(adminCookie, LABELS + "?missing=ko"), ErrorCode.INVALID_MISSING_LANGUAGE);
		expectError(getAs(adminCookie, LABELS + "?missing=ja"), ErrorCode.INVALID_MISSING_LANGUAGE);
		expectError(getAs(adminCookie, LABELS + "?missing=zh-CN"), ErrorCode.INVALID_MISSING_LANGUAGE);
		getAs(adminCookie, LABELS + "?size=0").andExpect(status().isBadRequest());
		getAs(adminCookie, LABELS + "?size=101").andExpect(status().isBadRequest());
		getAs(adminCookie, LABELS + "?keyword=" + "k".repeat(101)).andExpect(status().isBadRequest());
		expectError(getAs(adminCookie, LABELS + "?cursor=not-a-cursor"), ErrorCode.INVALID_CURSOR);
		getAs(adminCookie, LABELS + "?size=1&missing=").andExpect(status().isOk());
	}

	private I18nLabel saveLabel(final String key, final String ko, final String en, final String zhTw) {
		return i18nLabelRepository.save(I18nLabel.create(key, ko, en, zhTw));
	}

	private static List<Long> ids(final JsonNode page) {
		final List<Long> ids = new ArrayList<>();
		page.get("items").forEach(item -> ids.add(item.get("id").asLong()));
		return ids;
	}
}
