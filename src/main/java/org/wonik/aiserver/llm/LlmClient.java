package org.wonik.aiserver.llm;

/**
 * LLM에 지시문을 보내고 답을 받는 약속.
 * 기능(WBS, 업무일지 등)은 이 약속만 알고, 실제 통신 방식은 모른다.
 * 그래야 호출 방식이 바뀌어도 기능 코드를 고치지 않고, 테스트에서 가짜로 바꿔 끼울 수 있다.
 */
public interface LlmClient {

	/**
	 * 지시문을 보내고 LLM이 만든 글자 답을 돌려준다.
	 *
	 * @param systemPrompt LLM의 역할과 규칙을 알려 주는 지시문
	 * @param userPrompt   실제로 처리할 내용
	 * @return LLM이 만든 텍스트(JSON 해석은 호출한 쪽이 한다)
	 * @throws org.wonik.global.common.exception.CustomException
	 *         준비 안 됨·연결 실패 503, 시간 초과 504, 응답 형식 이상 502
	 */
	String chat(String systemPrompt, String userPrompt);
}
