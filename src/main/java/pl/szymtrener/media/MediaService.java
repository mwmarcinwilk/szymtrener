package pl.szymtrener.media;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.szymtrener.config.AppProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pliki trzymamy w bazie (media_blob). Zalety: jedna kopia zapasowa (pg_dump),
 * brak rozjazdu miedzy dyskiem a rekordem, przenosiny serwera to sam dump.
 * Koszt: baza rosnie i kazde pobranie idzie przez aplikacje — dlatego adresy sa
 * niezmienne i odpowiedzi maja `immutable` + ETag, wiec nginx/przegladarka
 * pytaja o plik dokladnie raz.
 */
@Service
public class MediaService {

    private final MediaRepository repository;
    private final MediaBlobRepository blobs;
    private final AppProperties props;
    private final Map<Long, String> urlCache = new ConcurrentHashMap<>();

    public MediaService(MediaRepository repository, MediaBlobRepository blobs, AppProperties props) {
        this.repository = repository;
        this.blobs = blobs;
        this.props = props;
    }

    @Transactional
    public MediaFile upload(MultipartFile file, String altText) throws IOException {
        String mime = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream");
        if (!Arrays.asList(props.media().allowedMime().split(",")).contains(mime)) {
            throw new IllegalArgumentException("Niedozwolony typ pliku: " + mime);
        }

        byte[] bytes = file.getBytes();
        MediaKind kind = mime.startsWith("image/") ? MediaKind.IMAGE
                : "application/pdf".equals(mime) ? MediaKind.PDF : MediaKind.OTHER;

        Integer width = null, height = null;
        String extension = extensionFor(mime);

        if (kind == MediaKind.IMAGE && !"image/webp".equals(mime)) {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
            if (source != null) {
                int max = props.media().maxImageWidth();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                if (source.getWidth() > max) {
                    Thumbnails.of(source).width(max).outputFormat("jpg")
                            .outputQuality(props.media().jpegQuality()).toOutputStream(out);
                    bytes = out.toByteArray();
                    extension = "jpg";
                    mime = "image/jpeg";
                    BufferedImage scaled = ImageIO.read(new ByteArrayInputStream(bytes));
                    width = scaled.getWidth();
                    height = scaled.getHeight();
                } else {
                    width = source.getWidth();
                    height = source.getHeight();
                }
            }
        }

        String checksum = sha256(bytes);
        Optional<MediaFile> duplicate = repository.findByChecksum(checksum);
        if (duplicate.isPresent()) return duplicate.get();   // ten sam plik wgrany drugi raz

        LocalDate today = LocalDate.now();
        String key = "%d/%02d/%s.%s".formatted(today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 16), extension);

        MediaFile media = new MediaFile();
        media.setStorageKey(key);
        media.setOriginalName(safeName(file.getOriginalFilename()));
        media.setMimeType(mime);
        media.setKind(kind);
        media.setSizeBytes(bytes.length);
        media.setWidth(width);
        media.setHeight(height);
        media.setAltText(altText);
        media.setChecksum(checksum);
        media = repository.save(media);
        blobs.save(new MediaBlob(media.getId(), bytes));
        return media;
    }

    /** Wariant dla plikow, ktore nie przyszly z formularza (np. obrazki z DOCX). */
    @Transactional
    public MediaFile store(byte[] bytes, String originalName, String mime, String altText) {
        String checksum = sha256(bytes);
        Optional<MediaFile> duplicate = repository.findByChecksum(checksum);
        if (duplicate.isPresent()) return duplicate.get();

        Integer width = null, height = null;
        if (mime.startsWith("image/")) {
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) { width = img.getWidth(); height = img.getHeight(); }
            } catch (IOException ignored) { /* nie blokujemy importu przez metadane */ }
        }

        LocalDate today = LocalDate.now();
        String key = "%d/%02d/%s.%s".formatted(today.getYear(), today.getMonthValue(),
                UUID.randomUUID().toString().replace("-", "").substring(0, 16), extensionFor(mime));

        MediaFile media = new MediaFile();
        media.setStorageKey(key);
        media.setOriginalName(safeName(originalName));
        media.setMimeType(mime);
        media.setKind(mime.startsWith("image/") ? MediaKind.IMAGE
                : "application/pdf".equals(mime) ? MediaKind.PDF : MediaKind.OTHER);
        media.setSizeBytes(bytes.length);
        media.setWidth(width);
        media.setHeight(height);
        media.setAltText(altText);
        media.setChecksum(checksum);
        media = repository.save(media);
        blobs.save(new MediaBlob(media.getId(), bytes));
        return media;
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> bytes(Long mediaId) {
        return blobs.findById(mediaId).map(MediaBlob::getData);
    }

    @Transactional(readOnly = true)
    public Optional<MediaFile> byStorageKey(String key) {
        return repository.findByStorageKey(key);
    }

    @Transactional(readOnly = true)
    public Optional<MediaFile> byId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public void countDownload(Long id) {
        repository.incrementDownloads(id);
    }

    /**
     * Usuwa plik razem z zawartoscia. Wolno wywolac dopiero po sprawdzeniu, ze
     * plik nie jest uzyty w zadnym wpisie — inaczej zostawimy w tresci puste miejsce.
     */
    @Transactional
    public void delete(Long id) {
        blobs.deleteById(id);
        repository.deleteById(id);
        urlCache.remove(id);   // mapa zakladala, ze pliki nie znikaja
    }

    /** Adres publiczny pliku; klucze sa niezmienne, wiec pamietamy je w mapie. */
    public String publicUrl(Long mediaId) {
        if (mediaId == null) return null;
        return urlCache.computeIfAbsent(mediaId,
                id -> repository.findById(id).map(MediaFile::publicUrl).orElse(null));
    }

    private static String extensionFor(String mime) {
        return switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }

    private static String safeName(String name) {
        if (name == null || name.isBlank()) return "plik";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
