package org.wonik.coreviaai_be.domain.i18n.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
public record LanguagePreferenceRequest(@Pattern(regexp = "ko|en|zh-TW") String language,
    @NotNull @PositiveOrZero Long version) {}
