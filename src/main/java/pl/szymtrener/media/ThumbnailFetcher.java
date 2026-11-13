package pl.szymtrener.media;

import java.util.Optional;

/** Pobiera miniature filmu YouTube po zweryfikowanym id. Za interfejsem, zeby testy nie chodzily do sieci. */
public interface ThumbnailFetcher {

    /** Bajty JPEG albo pusto, gdy pobranie sie nie udalo lub odpowiedz nie jest malym JPEG-iem. */
    Optional<byte[]> fetch(String videoId);
}
