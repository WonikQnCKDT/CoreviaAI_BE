package org.wonik.global.security;

/**
 * 신규 사용자 계정의 초기 임시 비밀번호를 발급해 저장한다.
 * <p>
 * 업무 모듈(coreviaai_be)은 로그인 자격 증명 저장 방식을 알지 않고 이 인터페이스만 통해 발급을 위임한다
 * ({@link UserSessionTerminator}와 같은 경계 분리 방식).
 */
public interface InitialPasswordIssuer {

	/**
	 * 현재 DB 트랜잭션 안에서 호출해 사용자와 자격 증명이 원자적으로 함께 저장되게 한다.
	 *
	 * @return 호출자에게 한 번만 전달할 임시 비밀번호 평문 (저장·로그 금지)
	 */
	String issueInitialPassword(Long userId);
}
