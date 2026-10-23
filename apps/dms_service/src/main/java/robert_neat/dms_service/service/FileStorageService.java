package robert_neat.dms_service.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import robert_neat.dms_service.config.DmsStorageProperties;
import robert_neat.dms_service.exception.BadRequestException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** Operacje na plikach wolumenu dms-data. Cała walidacja przeciw path traversal / zip-slip żyje
 * tutaj, w jednym miejscu, zamiast rozproszona po serwisach - każda ścieżka zapisu/odczytu
 * przechodzi przez resolveWithinBase przed dotknięciem dysku. */
@Service
public class FileStorageService {

    /** Bezpieczny identyfikator segmentu ścieżki (dokumentId, nazwy katalogów) - zgodny z varchar(64). */
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private final Path basePath;

    public FileStorageService(DmsStorageProperties storageProperties) {
        this.basePath = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();
    }

    /** Waliduje segment identyfikatora/nazwy użyty do budowy ścieżki (np. dokumentId). Odrzuca, nie czyści -
     * nazwa z path traversal musi skończyć się błędem 400, nie "naprawioną" ścieżką. */
    public void waliduj(String segment, String etykieta) {
        if (segment == null || !SAFE_SEGMENT.matcher(segment).matches()) {
            throw new BadRequestException("Niedozwolona wartość pola '" + etykieta + "': " + segment);
        }
    }

    /** Wyodrębnia bezpieczne rozszerzenie z oryginalnej nazwy pliku (tylko znaki alfanumeryczne). */
    public String rozszerzenie(String originalFilename) {
        if (originalFilename == null) {
            return "bin";
        }
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0 || idx == originalFilename.length() - 1) {
            return "bin";
        }
        String ext = originalFilename.substring(idx + 1);
        if (!ext.matches("[A-Za-z0-9]{1,16}")) {
            throw new BadRequestException("Niedozwolone rozszerzenie pliku: " + ext);
        }
        return ext;
    }

    /** Zapisuje zawartość pod relatywną ścieżką (względem base-path), tworząc katalogi nadrzędne.
     * Zwraca relatywną ścieżkę (zawsze z separatorem '/', niezależnie od OS) do zapisania w bazie. */
    public String zapisz(String relativePath, byte[] content) {
        Path target = resolveWithinBase(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się zapisać pliku: " + relativePath, e);
        }
        return relativePath;
    }

    public Resource wczytaj(String relativePath) {
        Path target = resolveWithinBase(relativePath);
        if (!Files.exists(target)) {
            return null;
        }
        try {
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** normalize() + startsWith(base) - jedyny punkt wejścia na dysk w tej klasie. Rzuca BadRequestException,
     * jeśli wynikowa ścieżka wychodzi poza katalog bazowy (path traversal / zip-slip). */
    private Path resolveWithinBase(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new BadRequestException("Brak ścieżki pliku.");
        }
        Path resolved;
        try {
            resolved = basePath.resolve(relativePath).normalize();
        } catch (InvalidPathException e) {
            // Np. znaki niedozwolone w ścieżce na danym OS (Windows: ?, *, <, >, |, ") - traktujemy
            // jak każdą inną niedozwoloną ścieżkę, nie jako błąd serwera.
            throw new BadRequestException("Niedozwolona ścieżka pliku: " + relativePath);
        }
        if (!resolved.startsWith(basePath)) {
            throw new BadRequestException("Niedozwolona ścieżka pliku: " + relativePath);
        }
        return resolved;
    }
}
