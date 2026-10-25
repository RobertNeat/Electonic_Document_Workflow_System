package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.administracja.TypOperacjiAudytu;
import robert_neat.document_api.dto.korespondencja.KanalWysylki;
import robert_neat.document_api.dto.korespondencja.Przesylka;
import robert_neat.document_api.dto.korespondencja.StatusPrzesylki;
import robert_neat.document_api.dto.korespondencja.UtworzPrzesylkeRequest;
import robert_neat.document_api.dto.korespondencja.WpisStatusuPrzesylki;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.ObywatelEntity;
import robert_neat.document_api.entity.PrzesylkaEntity;
import robert_neat.document_api.entity.SprawaEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.entity.WpisStatusuPrzesylkiEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.KorespondencjaMapper;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.ObywatelRepository;
import robert_neat.document_api.repository.PrzesylkaRepository;
import robert_neat.document_api.repository.SprawaRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;
import robert_neat.document_api.repository.WpisStatusuPrzesylkiRepository;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class KorespondencjaService {

    /** Wzorce numerów referencyjnych wywnioskowane z danych seedowych (100-seed.xml):
     * e-doreczenia -> "ADE/2026/00123" (ADE/{rok}/%05d), operator_pocztowy -> "PL2026000998"
     * (PL{rok}%06d). Brak seeda dla email - przyjęto analogiczny wzorzec "MAIL/{rok}/%05d" z
     * kontraktu (deklarowana dewiacja: nie ma potwierdzenia w danych demo, tylko w opisie kontraktu). */
    private static final Pattern NUMER_ADE_PATTERN = Pattern.compile("^ADE/\\d+/(\\d+)$");
    private static final Pattern NUMER_PL_PATTERN = Pattern.compile("^PL\\d{4}(\\d+)$");
    private static final Pattern NUMER_MAIL_PATTERN = Pattern.compile("^MAIL/\\d+/(\\d+)$");

    private final PrzesylkaRepository przesylkaRepository;
    private final WpisStatusuPrzesylkiRepository wpisStatusuPrzesylkiRepository;
    private final SprawaRepository sprawaRepository;
    private final DokumentRepository dokumentRepository;
    private final WersjaDokumentuRepository wersjaDokumentuRepository;
    private final ObywatelRepository obywatelRepository;
    private final AuditLogService auditLogService;

    public KorespondencjaService(PrzesylkaRepository przesylkaRepository,
                                  WpisStatusuPrzesylkiRepository wpisStatusuPrzesylkiRepository,
                                  SprawaRepository sprawaRepository,
                                  DokumentRepository dokumentRepository,
                                  WersjaDokumentuRepository wersjaDokumentuRepository,
                                  ObywatelRepository obywatelRepository,
                                  AuditLogService auditLogService) {
        this.przesylkaRepository = przesylkaRepository;
        this.wpisStatusuPrzesylkiRepository = wpisStatusuPrzesylkiRepository;
        this.sprawaRepository = sprawaRepository;
        this.dokumentRepository = dokumentRepository;
        this.wersjaDokumentuRepository = wersjaDokumentuRepository;
        this.obywatelRepository = obywatelRepository;
        this.auditLogService = auditLogService;
    }

    public List<Przesylka> listaPrzesylek(String status, String kanal, String kierunek, String obywatelId) {
        Specification<PrzesylkaEntity> spec = Specification.allOf(
                PrzesylkaSpecifications.statusEquals(status),
                PrzesylkaSpecifications.kanalEquals(kanal),
                PrzesylkaSpecifications.kierunekEquals(kierunek),
                PrzesylkaSpecifications.obywatelIdEquals(obywatelId)
        );
        List<PrzesylkaEntity> przesylki = przesylkaRepository.findAll(spec);
        Map<String, List<WpisStatusuPrzesylkiEntity>> historie = historieDlaPrzesylek(przesylki);
        return przesylki.stream()
                .map(p -> KorespondencjaMapper.toDto(p, historiaDto(historie, p.getId())))
                .toList();
    }

    private Map<String, List<WpisStatusuPrzesylkiEntity>> historieDlaPrzesylek(List<PrzesylkaEntity> przesylki) {
        List<String> ids = przesylki.stream().map(PrzesylkaEntity::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return wpisStatusuPrzesylkiRepository.findByPrzesylka_IdInOrderByDataAsc(ids).stream()
                .collect(Collectors.groupingBy(w -> w.getPrzesylka().getId()));
    }

    private List<WpisStatusuPrzesylki> historiaDto(Map<String, List<WpisStatusuPrzesylkiEntity>> historie, String przesylkaId) {
        return historie.getOrDefault(przesylkaId, List.of()).stream()
                .map(KorespondencjaMapper::toDto)
                .toList();
    }

    public Przesylka getById(String id) {
        PrzesylkaEntity przesylka = getEntityById(id);
        List<WpisStatusuPrzesylki> historia = wpisStatusuPrzesylkiRepository
                .findByPrzesylka_IdOrderByDataAsc(id).stream()
                .map(KorespondencjaMapper::toDto)
                .toList();
        return KorespondencjaMapper.toDto(przesylka, historia);
    }

    private PrzesylkaEntity getEntityById(String id) {
        return przesylkaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Przesyłka o id=" + id + " nie istnieje"));
    }

    @Transactional
    public Przesylka nadajPismo(UtworzPrzesylkeRequest request, String osoba) {
        SprawaEntity sprawa = sprawaRepository.findById(request.sprawaId())
                .orElseThrow(() -> new NotFoundException("Sprawa o id=" + request.sprawaId() + " nie istnieje"));
        DokumentEntity dokument = dokumentRepository.findById(request.dokumentId())
                .orElseThrow(() -> new NotFoundException("Dokument o id=" + request.dokumentId() + " nie istnieje"));
        ObywatelEntity obywatel = obywatelRepository.findById(request.obywatelId())
                .orElseThrow(() -> new NotFoundException("Obywatel o id=" + request.obywatelId() + " nie istnieje"));

        if (request.kanal() == KanalWysylki.E_DORECZENIA
                && (obywatel.getAdresDoreczenEDE() == null || obywatel.getAdresDoreczenEDE().isBlank())) {
            throw new BadRequestException(
                    "Obywatel o id=" + obywatel.getId() + " nie ma zarejestrowanego adresu do e-Doręczeń (ADE) - nie można nadać pisma tym kanałem.");
        }

        WersjaDokumentuEntity aktualnaWersja = wersjaDokumentuRepository
                .findByDokument_IdAndWersja(dokument.getId(), dokument.getAktualnaWersja())
                .orElseThrow(() -> new BadRequestException(
                        "Dokument o id=" + dokument.getId() + " nie ma aktualnej wersji (" + dokument.getAktualnaWersja() + ") - nie można nadać pisma"));
        if (!aktualnaWersja.isPodpisana()) {
            throw new BadRequestException(
                    "Aktualna wersja (" + aktualnaWersja.getWersja() + ") dokumentu o id=" + dokument.getId() + " nie jest podpisana - nie można nadać pisma");
        }

        OffsetDateTime teraz = OffsetDateTime.now();
        String numerReferencyjny = generujNumerReferencyjny(request.kanal());

        PrzesylkaEntity przesylka = new PrzesylkaEntity();
        przesylka.setId(UUID.randomUUID().toString());
        przesylka.setSprawa(sprawa);
        przesylka.setDokument(dokument);
        przesylka.setObywatel(obywatel);
        przesylka.setKanal(request.kanal().getWartosc());
        przesylka.setStatus(StatusPrzesylki.NADANE.getWartosc());
        przesylka.setNumerReferencyjny(numerReferencyjny);
        przesylka.setDataNadania(teraz);
        przesylka.setDataStatusu(teraz);
        przesylka.setKierunek("wychodzaca");
        przesylkaRepository.save(przesylka);

        WpisStatusuPrzesylkiEntity wpis = new WpisStatusuPrzesylkiEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setPrzesylka(przesylka);
        wpis.setStatus(StatusPrzesylki.NADANE.getWartosc());
        wpis.setData(teraz);
        wpisStatusuPrzesylkiRepository.save(wpis);

        auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.WYSYLKA_PISMA, osoba,
                "Przesyłka " + numerReferencyjny, "Nadano pismo kanałem " + request.kanal().getWartosc());

        return KorespondencjaMapper.toDto(przesylka, List.of(KorespondencjaMapper.toDto(wpis)));
    }

    /** Numer = max(numer istniejących przesyłek tego kanału+roku) + 1, nie licznik wierszy -
     * licznik wierszy psułby się przy nieciągłych numerach w seedzie (złamanie unique constraint,
     * analogicznie do SprawaService.generujZnakSprawy). */
    private String generujNumerReferencyjny(KanalWysylki kanal) {
        int rok = Year.now().getValue();
        return switch (kanal) {
            case E_DORECZENIA -> generujNumer("ADE/" + rok + "/%", NUMER_ADE_PATTERN, "ADE/" + rok + "/%05d");
            case OPERATOR_POCZTOWY -> generujNumer("PL" + rok + "%", NUMER_PL_PATTERN, "PL" + rok + "%06d");
            case EMAIL -> generujNumer("MAIL/" + rok + "/%", NUMER_MAIL_PATTERN, "MAIL/" + rok + "/%05d");
        };
    }

    private String generujNumer(String prefix, Pattern pattern, String format) {
        List<String> istniejace = przesylkaRepository.findNumeryReferencyjneByPrefix(prefix);
        int maxNumer = 0;
        for (String numer : istniejace) {
            Matcher m = pattern.matcher(numer);
            if (m.matches()) {
                int wartosc = Integer.parseInt(m.group(1));
                if (wartosc > maxNumer) {
                    maxNumer = wartosc;
                }
            }
        }
        return String.format(format, maxNumer + 1);
    }

    /** Wołane z PrzesylkaStatusuConsumer (odbiór zdarzenia RabbitMQ z delivery_mock) - nowyStatus
     * jest już zmapowany na enum przez wywołującego (wartość nierozpoznana jest łapana tam, przed
     * wejściem tutaj). Przesyłka jest odnajdywana i modyfikowana w TEJ SAMEJ transakcji (nie
     * przyjmowana jako already-loaded encja od wywołującego) - inaczej byłaby detached, bo konsument
     * RabbitMQ nie otwiera własnej transakcji. Data statusu stemplowana tutaj (OffsetDateTime.now(),
     * czas odbioru zdarzenia przez document_api), nie przyjmowana od nadawcy. Optional.empty(), gdy
     * numerReferencyjny nie istnieje - wywołujący (konsument) loguje i odrzuca wiadomość bez
     * ponownej kolejki. */
    @Transactional
    public Optional<Przesylka> aktualizujStatus(String numerReferencyjny, StatusPrzesylki nowyStatus) {
        PrzesylkaEntity przesylka = przesylkaRepository.findByNumerReferencyjny(numerReferencyjny).orElse(null);
        if (przesylka == null) {
            return Optional.empty();
        }
        OffsetDateTime teraz = OffsetDateTime.now();

        przesylka.setStatus(nowyStatus.getWartosc());
        przesylka.setDataStatusu(teraz);

        WpisStatusuPrzesylkiEntity wpis = new WpisStatusuPrzesylkiEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setPrzesylka(przesylka);
        wpis.setStatus(nowyStatus.getWartosc());
        wpis.setData(teraz);
        wpisStatusuPrzesylkiRepository.save(wpis);

        List<WpisStatusuPrzesylki> historia = wpisStatusuPrzesylkiRepository
                .findByPrzesylka_IdOrderByDataAsc(przesylka.getId()).stream()
                .map(KorespondencjaMapper::toDto)
                .toList();
        return Optional.of(KorespondencjaMapper.toDto(przesylka, historia));
    }
}
