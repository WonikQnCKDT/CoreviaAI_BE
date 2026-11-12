package org.wonik.global.audit;

/**
 * 감사 로그의 행위자. 컨트롤러는 파라미터로 선언만 하고 값은 {@link org.wonik.global.config.web.AuditActorArgumentResolver}가
 * 채운다(행위자·IP를 얻는 방식을 한 곳에서 관리하기 위해).
 *
 * @param userId    행위자 사용자 ID
 * @param ipAddress 접속 IP(nginx X-Forwarded-For 기준)
 */
public record AuditActor(Long userId, String ipAddress) {
}
