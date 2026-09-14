package pl.szymtrener.crm;

/** Jak odebrana wiadomosc trafila do watku. Naglowek From da sie podrobic, stad rozroznienie. */
public enum MatchedBy {
    /** In-Reply-To/References wskazuje nasz Message-ID. */
    REPLY,
    /** Tylko adres nadawcy zgadza sie z klientem albo zgloszeniem. */
    ADDRESS
}
