package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import robert_neat.document_api.dto.administracja.TypOperacjiAudytu;
import robert_neat.document_api.dto.platnosci.DopasujWplateRequest;
import robert_neat.document_api.dto.platnosci.Naleznosc;
import robert_neat.document_api.dto.platnosci.NaleznoscListElement;
import robert_neat.document_api.dto.platnosci.StatusRozliczenia;
import robert_neat.document_api.dto.platnosci.Wplata;
import robert_neat.document_api.dto.platnosci.WynikImportuCamt053;
import robert_neat.document_api.entity.NaleznoscEntity;
import robert_neat.document_api.entity.WplataEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.PlatnosciMapper;
import robert_neat.document_api.repository.NaleznoscRepository;
import robert_neat.document_api.repository.WplataRepository;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PlatnosciService {

    private final NaleznoscRepository naleznoscRepository;
    private final WplataRepository wplataRepository;
    private final AuditLogService auditLogService;

    public PlatnosciService(NaleznoscRepository naleznoscRepository, WplataRepository wplataRepository,
                             AuditLogService auditLogService) {
        this.naleznoscRepository = naleznoscRepository;
        this.wplataRepository = wplataRepository;
        this.auditLogService = auditLogService;
    }

    // --- naleznosci ---

    public List<NaleznoscListElement> listaNaleznosci(String status, String obywatelId) {
        Specification<NaleznoscEntity> spec = Specification.allOf(
                NaleznoscSpecifications.statusEquals(status),
                NaleznoscSpecifications.obywatelIdEquals(obywatelId));
        return naleznoscRepository.findAll(spec).stream()
                .map(n -> PlatnosciMapper.toListElement(n, n.getSprawa().getZnakSprawy(), PlatnosciMapper.obywatelNazwa(n.getObywatel())))
                .toList();
    }

    public Naleznosc getById(String id) {
        return PlatnosciMapper.toDto(getEntityById(id));
    }

    private NaleznoscEntity getEntityById(String id) {
        return naleznoscRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Należność o id=" + id + " nie istnieje"));
    }

    // --- wplaty ---

    public List<Wplata> listaWplat(String naleznoscId, String obywatelId) {
        Specification<WplataEntity> spec = Specification.allOf(
                WplataSpecifications.naleznoscIdEquals(naleznoscId),
                WplataSpecifications.obywatelIdEquals(obywatelId));
        return wplataRepository.findAll(spec).stream().map(PlatnosciMapper::toDto).toList();
    }

    @Transactional
    public Wplata dopasujWplate(String wplataId, DopasujWplateRequest request, String osoba) {
        WplataEntity wplata = wplataRepository.findById(wplataId)
                .orElseThrow(() -> new NotFoundException("Wpłata o id=" + wplataId + " nie istnieje"));
        NaleznoscEntity naleznosc = getEntityById(request.naleznoscId());

        wplata.setNaleznosc(naleznosc);
        wplataRepository.save(wplata);
        przeliczRozliczenie(naleznosc);

        auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.REJESTRACJA_WPLATY, osoba,
                "Wpłata " + wplataId, "Dopasowano do należności " + naleznosc.getNumerReferencyjny());

        return PlatnosciMapper.toDto(wplata);
    }

    /** Przelicza kwotaRozliczona jako sumę WSZYSTKICH wpłat obecnie dopasowanych do danej należności
     * (nie tylko tej właśnie dopasowywanej - należność może mieć wiele częściowych wpłat), a następnie
     * wyprowadza status z czterech możliwych wartości enuma (reguła biznesowa nie opisana explicite
     * w kontrakcie, wywnioskowana z nazw wartości CHECK constraint):
     *   - kwotaRozliczona >= kwota            -> rozliczona
     *   - 0 < kwotaRozliczona < kwota          -> czesciowo_rozliczona
     *   - kwotaRozliczona == 0 i po terminie   -> przeterminowana
     *   - kwotaRozliczona == 0 i przed terminem -> nierozliczona
     */
    private void przeliczRozliczenie(NaleznoscEntity naleznosc) {
        BigDecimal suma = wplataRepository.findByNaleznosc_Id(naleznosc.getId()).stream()
                .map(WplataEntity::getKwota)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        naleznosc.setKwotaRozliczona(suma);

        StatusRozliczenia nowyStatus;
        if (suma.compareTo(naleznosc.getKwota()) >= 0) {
            nowyStatus = StatusRozliczenia.ROZLICZONA;
        } else if (suma.compareTo(BigDecimal.ZERO) > 0) {
            nowyStatus = StatusRozliczenia.CZESCIOWO_ROZLICZONA;
        } else if (naleznosc.getTerminPlatnosci().isBefore(LocalDate.now())) {
            nowyStatus = StatusRozliczenia.PRZETERMINOWANA;
        } else {
            nowyStatus = StatusRozliczenia.NIEROZLICZONA;
        }
        naleznosc.setStatus(nowyStatus.getWartosc());
        naleznoscRepository.save(naleznosc);
    }

    // --- import CAMT.053 ---

    @Transactional
    public WynikImportuCamt053 importujCamt053(InputStream xml, String osoba) {
        List<WplataCamt> wplatyZWyciagu = parsujCamt053(xml);

        int liczbaDopasowanychAutomatycznie = 0;
        List<Wplata> utworzoneWplaty = new java.util.ArrayList<>();

        for (WplataCamt wz : wplatyZWyciagu) {
            WplataEntity wplata = new WplataEntity();
            wplata.setId(UUID.randomUUID().toString());
            wplata.setKwota(wz.kwota());
            wplata.setData(wz.data());
            wplata.setTytul(wz.tytul());
            wplata.setKontrahent(wz.kontrahent());

            NaleznoscEntity dopasowana = znajdzJedynegoKandydata(wz);
            if (dopasowana != null) {
                wplata.setNaleznosc(dopasowana);
                wplata.setDopasowanaAutomatycznie(true);
                liczbaDopasowanychAutomatycznie++;
            } else {
                wplata.setDopasowanaAutomatycznie(false);
            }
            wplataRepository.save(wplata);
            if (dopasowana != null) {
                przeliczRozliczenie(dopasowana);
            }
            utworzoneWplaty.add(PlatnosciMapper.toDto(wplata));
        }

        int liczbaTransakcji = wplatyZWyciagu.size();
        int liczbaDoRekoncyliacji = liczbaTransakcji - liczbaDopasowanychAutomatycznie;

        // Jeden zbiorczy wpis dla całego importu, nie po jednym na wpłatę - unika zalewania
        // dziennika zdarzeń przy dużych wyciągach bankowych.
        auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.REJESTRACJA_WPLATY, osoba,
                "Import CAMT.053",
                "Import CAMT.053: " + liczbaTransakcji + " transakcji, " + liczbaDopasowanychAutomatycznie + " dopasowanych automatycznie");

        return new WynikImportuCamt053(liczbaTransakcji, liczbaDopasowanychAutomatycznie, liczbaDoRekoncyliacji, utworzoneWplaty);
    }

    /** Dopasowanie automatyczne tylko gdy TRAFIA DOKŁADNIE JEDEN kandydat (albo po numerze referencyjnym
     * jako podciąg tekstu uznania, albo po dokładnej równości kwoty z brakującą kwotą należności) wśród
     * należności jeszcze nie w pełni rozliczonych (nierozliczona/czesciowo_rozliczona) - wieloznaczność
     * (0 lub >1 kandydatów) zostaje świadomie nierozwiązana i skierowana do rekoncyliacji ręcznej. */
    private NaleznoscEntity znajdzJedynegoKandydata(WplataCamt wz) {
        List<NaleznoscEntity> kandydaci = naleznoscRepository.findAll().stream()
                .filter(n -> "nierozliczona".equals(n.getStatus()) || "czesciowo_rozliczona".equals(n.getStatus()))
                .filter(n -> dopasowujeSie(n, wz))
                .toList();
        return kandydaci.size() == 1 ? kandydaci.get(0) : null;
    }

    private boolean dopasowujeSie(NaleznoscEntity n, WplataCamt wz) {
        if (wz.referencja() != null && !wz.referencja().isBlank()
                && wz.referencja().contains(n.getNumerReferencyjny())) {
            return true;
        }
        BigDecimal brakujaca = n.getKwota().subtract(n.getKwotaRozliczona());
        return brakujaca.compareTo(wz.kwota()) == 0;
    }

    /** Parsuje camt.053.001.xx (Document/BkToCstmrStmt/Stmt/Ntry) przy użyciu JDK DOM, filtrując do
     * wpisów kredytowych (CdtDbtInd=CRDT) - to jedyne wpływy istotne dla rozliczania należności.
     * Ścieżki elementów na tekst referencyjny sprawdzane w kolejności od najbardziej specyficznej:
     * NtryDtls/TxDtls/RmtInf/Ustrd (nieustrukturyzowany tytuł płatności - zwykle zawiera numer
     * referencyjny należności wpisany przez płatnika), potem AddtlNtryInf (dodatkowa informacja na
     * poziomie wpisu, używana przez część banków gdy nie ma szczegółów transakcji), potem NtryRef
     * (referencja samego wpisu - ostatni fallback, rzadko zawiera numer należności, ale bywa jedynym
     * dostępnym polem) - różni producenci camt.053 wypełniają różne pola, stąd defensywne sprawdzanie
     * wielu ścieżek zamiast jednej "kanonicznej".
     * XXE: DOCTYPE i zewnętrzne encje wyłączone, bo wejście jest nieuwierzytelnionym plikiem użytkownika. */
    private List<WplataCamt> parsujCamt053(InputStream xml) {
        Document document = parseSecurely(xml);
        NodeList wpisy = document.getElementsByTagName("Ntry");
        List<WplataCamt> wynik = new java.util.ArrayList<>();

        for (int i = 0; i < wpisy.getLength(); i++) {
            Element ntry = (Element) wpisy.item(i);
            String kierunek = tekstDziecka(ntry, "CdtDbtInd");
            if (!"CRDT".equals(kierunek)) {
                continue;
            }

            BigDecimal kwota = kwotaZWpisu(ntry);
            LocalDate data = dataZWpisu(ntry);
            String referencja = referencjaZWpisu(ntry);
            String kontrahent = kontrahentZWpisu(ntry);

            String tytul = (referencja == null || referencja.isBlank()) ? "Wpłata z wyciągu CAMT.053" : referencja;
            wynik.add(new WplataCamt(kwota, data, tytul, kontrahent, referencja));
        }
        return wynik;
    }

    private Document parseSecurely(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(xml);
        } catch (Exception e) {
            throw new BadRequestException("Nie udało się przetworzyć pliku CAMT.053: " + e.getMessage());
        }
    }

    private BigDecimal kwotaZWpisu(Element ntry) {
        NodeList amt = ntry.getElementsByTagName("Amt");
        if (amt.getLength() == 0) {
            throw new BadRequestException("Wpis Ntry bez elementu Amt - nieprawidłowy plik CAMT.053");
        }
        return new BigDecimal(amt.item(0).getTextContent().trim());
    }

    private LocalDate dataZWpisu(Element ntry) {
        Element bookgDt = pierwszeDziecko(ntry, "BookgDt");
        Element valDt = pierwszeDziecko(ntry, "ValDt");
        Element zrodlo = bookgDt != null ? bookgDt : valDt;
        if (zrodlo == null) {
            return LocalDate.now();
        }
        String dt = tekstDziecka(zrodlo, "Dt");
        if (dt == null || dt.isBlank()) {
            String dtTm = tekstDziecka(zrodlo, "DtTm");
            if (dtTm != null && dtTm.length() >= 10) {
                return LocalDate.parse(dtTm.substring(0, 10));
            }
            return LocalDate.now();
        }
        return LocalDate.parse(dt.trim());
    }

    private String referencjaZWpisu(Element ntry) {
        NodeList rmtInf = ntry.getElementsByTagName("RmtInf");
        if (rmtInf.getLength() > 0) {
            String ustrd = tekstDziecka((Element) rmtInf.item(0), "Ustrd");
            if (ustrd != null && !ustrd.isBlank()) {
                return ustrd.trim();
            }
        }
        String addtlNtryInf = tekstDziecka(ntry, "AddtlNtryInf");
        if (addtlNtryInf != null && !addtlNtryInf.isBlank()) {
            return addtlNtryInf.trim();
        }
        String ntryRef = tekstDziecka(ntry, "NtryRef");
        if (ntryRef != null && !ntryRef.isBlank()) {
            return ntryRef.trim();
        }
        return null;
    }

    private String kontrahentZWpisu(Element ntry) {
        NodeList dbtr = ntry.getElementsByTagName("Dbtr");
        if (dbtr.getLength() > 0) {
            String nm = tekstDziecka((Element) dbtr.item(0), "Nm");
            if (nm != null && !nm.isBlank()) {
                return nm.trim();
            }
        }
        return "Nieznany";
    }

    /** Pierwsze dziecko o danej nazwie tagu bezpośrednio pod elementem (nie getElementsByTagName,
     * żeby nie złapać zagnieżdżonego elementu o tej samej nazwie z głębszego poziomu, np. Dt
     * występuje i pod BookgDt i pod ValDt). */
    private Element pierwszeDziecko(Element rodzic, String tag) {
        NodeList dzieci = rodzic.getChildNodes();
        for (int i = 0; i < dzieci.getLength(); i++) {
            if (dzieci.item(i) instanceof Element el && tag.equals(el.getTagName())) {
                return el;
            }
        }
        return null;
    }

    private String tekstDziecka(Element rodzic, String tag) {
        Element dziecko = pierwszeDziecko(rodzic, tag);
        return dziecko == null ? null : dziecko.getTextContent();
    }

    private record WplataCamt(BigDecimal kwota, LocalDate data, String tytul, String kontrahent, String referencja) {
    }
}
