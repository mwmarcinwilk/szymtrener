package pl.szymtrener.docimport;

import java.util.List;

/** Wynik importu dokumentu: gotowy HTML + co sie nie przenioslo. */
public record ImportResult(String html, int imageCount, List<String> warnings) {}
