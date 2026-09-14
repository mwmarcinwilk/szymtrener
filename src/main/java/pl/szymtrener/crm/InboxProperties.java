package pl.szymtrener.crm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Odbior odpowiedzi klientow ze skrzynki. Login i haslo sa te same co do wysylki
 * (spring.mail.username/password), wiec tu tylko serwer IMAP i wlacznik.
 *
 * Wartosci domyslne (OVH, 993, wylaczony) sa w application.yml.
 *
 * @param enabled domyslnie wylaczony: lokalnie i w testach aplikacja nie ma prawa laczyc sie z poczta
 */
@ConfigurationProperties(prefix = "app.inbox")
public record InboxProperties(boolean enabled, String host, int port) {}
