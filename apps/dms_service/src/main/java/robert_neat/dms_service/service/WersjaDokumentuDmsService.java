package robert_neat.dms_service.service;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.dms_service.entity.WersjaDokumentuDmsEntity;
import robert_neat.dms_service.exception.BadRequestException;
import robert_neat.dms_service.exception.NotFoundException;
import robert_neat.dms_service.repository.WersjaDokumentuDmsRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class WersjaDokumentuDmsService {

    private final WersjaDokumentuDmsRepository repository;
    private final FileStorageService fileStorageService;

    public WersjaDokumentuDmsService(WersjaDokumentuDmsRepository repository, FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    public List<WersjaDokumentuDmsEntity> listaWersji(String dokumentId) {
        fileStorageService.waliduj(dokumentId, "dokumentId");
        return repository.findByDokumentIdOrderByWersjaAsc(dokumentId);
    }

    public WersjaDokumentuDmsEntity getWersja(String dokumentId, int wersja) {
        fileStorageService.waliduj(dokumentId, "dokumentId");
        return repository.findByDokumentIdAndWersja(dokumentId, wersja)
                .orElseThrow(() -> new NotFoundException(
                        "Wersja " + wersja + " dokumentu " + dokumentId + " nie istnieje."));
    }

    public Resource wczytajPlik(String dokumentId, int wersja) {
        WersjaDokumentuDmsEntity entity = getWersja(dokumentId, wersja);
        Resource resource = fileStorageService.wczytaj(entity.getSciezkaPliku());
        if (resource == null) {
            throw new NotFoundException("Plik wersji " + wersja + " dokumentu " + dokumentId + " nie istnieje na dysku.");
        }
        return resource;
    }

    /** nowaWersja = MAX(wersja istniejących)+1 (1 jeśli brak). Zapis do bazy (saveAndFlush) PRZED
     * zapisem pliku: unique(dokument_id, wersja) odrzuci konkurencyjny upload obliczający to samo
     * MAX+1 zanim nadpisze plik innej wersji. */
    @Transactional
    public WersjaDokumentuDmsEntity uploadWersja(String dokumentId, String autor, MultipartFile plik) {
        fileStorageService.waliduj(dokumentId, "dokumentId");
        if (autor == null || autor.isBlank()) {
            throw new BadRequestException("autor jest wymagany.");
        }
        if (plik == null || plik.isEmpty()) {
            throw new BadRequestException("Plik dokumentu jest wymagany.");
        }

        int nowaWersja = repository.znajdzMaksWersje(dokumentId).orElse(0) + 1;
        String rozszerzenie = fileStorageService.rozszerzenie(plik.getOriginalFilename());
        String relativePath = "dokumenty/" + dokumentId + "/wd-" + nowaWersja + "." + rozszerzenie;

        byte[] content;
        try {
            content = plik.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        WersjaDokumentuDmsEntity entity = new WersjaDokumentuDmsEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setDokumentId(dokumentId);
        entity.setWersja(nowaWersja);
        entity.setSciezkaPliku(relativePath);
        entity.setPodpisana(false);
        entity.setDataUtworzenia(LocalDate.now());
        entity.setAutor(autor);
        entity.setStatusPodpisu("brak");
        repository.saveAndFlush(entity);

        fileStorageService.zapisz(relativePath, content);

        return entity;
    }
}
