package pl.szymtrener.crm;

import org.springframework.stereotype.Component;

/**
 * Limity zalacznikow dla widokow. W atrybutach th:data-* Thymeleaf dziala w trybie ograniczonym:
 * zabrania {@code T(...)}, a od 3.1.5 (CVE-2026-41901) takze odwolan do beanow {@code @nazwa}.
 * Dlatego bean pobiera {@code th:with="lim=${@attachmentLimits}"} (tryb zwykly), a th:data-* czytaja
 * juz zmienna {@code lim}. Blad w polowie widoku urywa strone przy statusie 200 (ThreadPagesRenderIT).
 */
@Component("attachmentLimits")
public class AttachmentLimits {

    public int maxFiles() { return AttachmentPolicy.OUT_MAX_FILES; }

    public long maxTotalBytes() { return AttachmentPolicy.OUT_MAX_TOTAL; }

    public long maxFileBytes() { return AttachmentPolicy.OUT_MAX_FILE; }
}
