package org.wonik.coreviaai_be.domain.i18n.service;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.i18n.entity.type.LabelLanguage;
import org.wonik.coreviaai_be.domain.i18n.service.result.LanguagePreference;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LanguagePreferenceService {
    private final UserRepository users;
    private final AuditLogger audit;
    public LanguagePreference get(Long userId) { return preference(user(userId)); }
    @Transactional
    public LanguagePreference update(AuditActor actor, String language, long version) {
        final User user = user(actor.userId());
        if (Objects.equals(user.getPreferredLanguage(), language)) return preference(user);
        if (user.getVersion() != version) throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
        final String before = user.getPreferredLanguage();
        user.changePreferredLanguage(language);
        users.flush();
        audit.record(actor, AuditAction.USER_LANGUAGE_CHANGED, AuditTargetType.USER, user.getId(),
            AuditChanges.create().change("preferredLanguage", before, language));
        return preference(user);
    }
    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
    private LanguagePreference preference(User user) {
        return new LanguagePreference(user.getPreferredLanguage(),
            user.getPreferredLanguage() == null ? LabelLanguage.KO.tag() : user.getPreferredLanguage(), user.getVersion());
    }
}
