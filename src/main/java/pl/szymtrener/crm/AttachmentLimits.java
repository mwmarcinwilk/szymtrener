package pl.szymtrener.crm;

import org.springframework.stereotype.Component;

/**
 * Limity zalacznikow dla widokow ({@code ${@attachmentLimits.maxFiles()}}). Przez bean, nie przez
 * {@code T(AttachmentPolicy)}: w atrybutach th:data-* Thymeleaf dziala w trybie ograniczonym i zabrania
 * dostepu do klas statycznych, a blad w polowie widoku urywa strone przy statusie 200.
 */
@Component("attachmentLimits")
public class AttachmentLimits {

    public int maxFiles() { return AttachmentPolicy.OUT_MAX_FILES; }

    public long maxTotalBytes() { return AttachmentPolicy.OUT_MAX_TOTAL; }

    public long maxFileBytes() { return AttachmentPolicy.OUT_MAX_FILE; }
}
