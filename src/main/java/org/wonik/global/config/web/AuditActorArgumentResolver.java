package org.wonik.global.config.web;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.jwt.LoginUser;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 컨트롤러 파라미터 {@link AuditActor}를 로그인 사용자와 접속 IP로 채운다.
 * 감사 로그를 남기는 모든 컨트롤러가 같은 방식으로 행위자를 얻도록 한 곳에 둔다.
 * 접속 IP는 forward-headers-strategy=framework 설정으로 X-Forwarded-For가 반영된 remoteAddr이다.
 */
public class AuditActorArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(final MethodParameter parameter) {
		return AuditActor.class.equals(parameter.getParameterType());
	}

	@Override
	public AuditActor resolveArgument(
		final MethodParameter parameter,
		final ModelAndViewContainer mavContainer,
		final NativeWebRequest webRequest,
		final WebDataBinderFactory binderFactory
	) {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof final LoginUser loginUser)) {
			// 인증이 필요한 경로에서만 쓰므로 여기 도달하면 보안 설정 오류다. 행위자 없는 감사 로그를 남기지 않는다.
			throw new IllegalStateException("감사 로그 행위자를 확인할 수 없습니다: 인증되지 않은 요청");
		}
		final HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		return new AuditActor(loginUser.userId(), request == null ? null : request.getRemoteAddr());
	}
}
