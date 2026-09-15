package pl.szymtrener.crm;

/**
 * Plik gotowy do zapisu w watku: nazwa juz oczyszczona, typ rozpoznany po tresci.
 * Tworzy go wylacznie {@link AttachmentPolicy}.
 */
public record AttachmentFile(String name, String mimeType, byte[] data) {

    @Override
    public String toString() {
        return "AttachmentFile[" + name + ", " + mimeType + ", " + data.length + " B]";
    }
}
