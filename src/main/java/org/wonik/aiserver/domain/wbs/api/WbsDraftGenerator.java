package org.wonik.aiserver.domain.wbs.api;

/**
 * aiserver가 업무 모듈에 공개하는 WBS 초안 생성 계약(FR-PRJ-07, 02. AI 생성 API의 POST /generate/wbs).
 * <p>
 * 업무 모듈(coreviaai_be)은 이 패키지(api)만 쓰고 aiserver의 구현 패키지는 import하지 않는다. aiserver는 업무 모듈을
 * import하지 않는다. 권한 확인과 결과 저장은 업무 모듈 책임이고, 이 계약은 초안을 만들어 돌려주기만 한다(저장 없음).
 * <p>
 * 실패는 {@code CustomException}으로 알린다: 생성 실패·형식 오류 502 {@code AI_GENERATION_FAILED},
 * 시간 초과 504 {@code AI_TIMEOUT}, 사용 불가 503.
 */
public interface WbsDraftGenerator {

	AiWbsDraftResult generate(AiWbsGenerateRequest request);
}
