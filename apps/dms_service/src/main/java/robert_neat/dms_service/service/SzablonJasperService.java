package robert_neat.dms_service.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.dms_service.entity.SzablonJasperEntity;
import robert_neat.dms_service.exception.BadRequestException;
import robert_neat.dms_service.exception.NotFoundException;
import robert_neat.dms_service.repository.SzablonJasperRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class SzablonJasperService {

    private final SzablonJasperRepository repository;
    private final FileStorageService fileStorageService;

    public SzablonJasperService(SzablonJasperRepository repository, FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    public List<SzablonJasperEntity> listaSzablonow() {
        return repository.findAll();
    }

    public SzablonJasperEntity getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Szablon o id " + id + " nie istnieje."));
    }

    public Resource wczytajPlik(String id) {
        SzablonJasperEntity szablon = getById(id);
        Resource resource = fileStorageService.wczytaj(szablon.getSciezkaPliku());
        if (resource == null) {
            throw new NotFoundException("Plik szablonu " + id + " nie istnieje na dysku.");
        }
        return resource;
    }

    /** Wersjonowanie per (typDokumentuId, nazwa): kolejny upload tej samej nazwy dla tego typu
     * dokumentu dostaje wersja = MAX(wersja)+1, zamiast nadpisywać poprzedni wiersz. Identyfikator
     * wpisu w nazwie pliku na dysku zapobiega kolizji nazw między różnymi szablonami/typami
     * dokumentu, które współdzielą oryginalną nazwę pliku. */
    @Transactional
    public SzablonJasperEntity uploadSzablon(String typDokumentuId, String nazwa, String autor, MultipartFile plik) {
        if (typDokumentuId == null || typDokumentuId.isBlank()) {
            throw new BadRequestException("typDokumentuId jest wymagane.");
        }
        if (nazwa == null || nazwa.isBlank()) {
            throw new BadRequestException("nazwa jest wymagana.");
        }
        if (autor == null || autor.isBlank()) {
            throw new BadRequestException("autor jest wymagany.");
        }
        if (plik == null || plik.isEmpty()) {
            throw new BadRequestException("Plik szablonu jest wymagany.");
        }

        int nowaWersja = repository.znajdzMaksWersje(typDokumentuId, nazwa).orElse(0) + 1;

        String id = UUID.randomUUID().toString();
        // Plik na dysku nazywany wg oryginalnej nazwy uploadu (zgodnie ze specyfikacją), nie wg pola
        // 'nazwa' formularza - to oryginalna nazwa pliku jest wektorem path traversal, nie 'nazwa'.
        String bezpiecznaNazwaPliku = sanitizujNazwePliku(plik.getOriginalFilename());
        String relativePath = "szablony/" + id + "_" + bezpiecznaNazwaPliku;

        byte[] content;
        try {
            content = plik.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        SzablonJasperEntity entity = new SzablonJasperEntity();
        entity.setId(id);
        entity.setTypDokumentuId(typDokumentuId);
        entity.setNazwa(nazwa);
        entity.setWersja(nowaWersja);
        entity.setDataWgrania(LocalDate.now());
        entity.setAutor(autor);
        entity.setSciezkaPliku(relativePath);
        repository.saveAndFlush(entity);

        // Zapis fizycznego pliku PO flush - jeśli wiersz w bazie nie powstanie (np. naruszenie
        // ograniczenia), plik nie zostanie osierocony na dysku.
        fileStorageService.zapisz(relativePath, content);

        return entity;
    }

    /** Odrzuca (nie czyści) niebezpieczne/nieprawidłowe nazwy oryginalnego pliku uploadu - '..', '/',
     * '\', ':' to realny wektor path traversal przy budowaniu ścieżki zapisu. Wymaga też rozszerzenia
     * .jrxml (walidacja typu pliku szablonu Jasper). */
    private String sanitizujNazwePliku(String nazwa) {
        if (nazwa == null || nazwa.isBlank()
                || nazwa.contains("..") || nazwa.contains("/") || nazwa.contains("\\") || nazwa.contains(":")) {
            throw new BadRequestException("Niedozwolona nazwa pliku: " + nazwa);
        }
        if (!nazwa.toLowerCase().endsWith(".jrxml")) {
            throw new BadRequestException("Szablon musi być plikiem .jrxml: " + nazwa);
        }
        return nazwa;
    }
}
