package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.client.DmsServiceClient;
import robert_neat.document_api.client.SignServiceClient;
import robert_neat.document_api.dto.administracja.TypOperacjiAudytu;
import robert_neat.document_api.dto.dokumenty.Dokument;
import robert_neat.document_api.dto.dokumenty.DokumentListElement;
import robert_neat.document_api.dto.dokumenty.StatusPodpisu;
import robert_neat.document_api.dto.dokumenty.SzablonJasper;
import robert_neat.document_api.dto.dokumenty.TypDokumentu;
import robert_neat.document_api.dto.dokumenty.WersjaDokumentu;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.DokumentyMapper;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.SzablonJasperRepository;
import robert_neat.document_api.repository.TypDokumentuRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DokumentService {

    private final DokumentRepository dokumentRepository;
    private final WersjaDokumentuRepository wersjaDokumentuRepository;
    private final TypDokumentuRepository typDokumentuRepository;
    private final SzablonJasperRepository szablonJasperRepository;
    private final DmsServiceClient dmsServiceClient;
    private final SignServiceClient signServiceClient;
    private final AuditLogService auditLogService;

    public DokumentService(DokumentRepository dokumentRepository,
                            WersjaDokumentuRepository wersjaDokumentuRepository,
                            TypDokumentuRepository typDokumentuRepository,
                            SzablonJasperRepository szablonJasperRepository,
                            DmsServiceClient dmsServiceClient,
                            SignServiceClient signServiceClient,
                            AuditLogService auditLogService) {
        this.dokumentRepository = dokumentRepository;
        this.wersjaDokumentuRepository = wersjaDokumentuRepository;
        this.typDokumentuRepository = typDokumentuRepository;
        this.szablonJasperRepository = szablonJasperRepository;
        this.dmsServiceClient = dmsServiceClient;
        this.signServiceClient = signServiceClient;
        this.auditLogService = auditLogService;
    }

    public List<TypDokumentu> listaTypowDokumentow() {
        return typDokumentuRepository.findAll().stream().map(DokumentyMapper::toDto).toList();
    }

    public List<SzablonJasper> listaSzablonow(String typDokumentuId) {
        List<robert_neat.document_api.entity.SzablonJasperEntity> szablony =
                (typDokumentuId == null || typDokumentuId.isBlank())
                        ? szablonJasperRepository.findAll()
                        : szablonJasperRepository.findByTypDokumentu_Id(typDokumentuId);
        return szablony.stream().map(DokumentyMapper::toDto).toList();
    }

    /** "Historia" szablonu = wszystkie wersje tej samej nazwy pliku + typu dokumentu, malejąco po
     * numerze wersji - schema nie ma osobnej tabeli historii zmian z "opisem zmiany"
     * (zob. scoping note w raporcie koncowym). */
    public List<SzablonJasper> historiaSzablonu(String id) {
        var szablon = szablonJasperRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Szablon o id=" + id + " nie istnieje"));
        return szablonJasperRepository
                .findByNazwaAndTypDokumentu_IdOrderByWersjaDesc(szablon.getNazwa(), szablon.getTypDokumentu().getId())
                .stream().map(DokumentyMapper::toDto).toList();
    }

    public List<DokumentListElement> listaDokumentow(String sprawaId, String typDokumentuId) {
        Specification<DokumentEntity> spec = Specification.allOf(
                DokumentSpecifications.sprawaIdEquals(sprawaId),
                DokumentSpecifications.typDokumentuIdEquals(typDokumentuId)
        );
        List<DokumentEntity> dokumenty = dokumentRepository.findAll(spec);
        Map<String, StatusPodpisu> statusyAktualnych = statusyPodpisuAktualnychWersji(dokumenty);
        return dokumenty.stream()
                .map(d -> DokumentyMapper.toListElement(
                        d, d.getSprawa().getZnakSprawy(),
                        statusyAktualnych.getOrDefault(d.getId(), StatusPodpisu.BRAK)))
                .toList();
    }

    /** Jedno zapytanie po wszystkich dokumentach z listy, potem filtrowanie w pamięci do wersji
     * == aktualnaWersja danego dokumentu - unika N+1 bez dedykowanego zapytania z MAX per-wiersz.
     * Dokument bez odpowiadającej wersji (np. brak wiersza wersja_dokumentu) dostaje BRAK. */
    private Map<String, StatusPodpisu> statusyPodpisuAktualnychWersji(List<DokumentEntity> dokumenty) {
        List<String> ids = dokumenty.stream().map(DokumentEntity::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<String, Integer> aktualneWersje = dokumenty.stream()
                .collect(Collectors.toMap(DokumentEntity::getId, DokumentEntity::getAktualnaWersja));
        List<WersjaDokumentuEntity> wersje = wersjaDokumentuRepository.findByDokument_IdIn(ids);
        return wersje.stream()
                .filter(w -> w.getWersja() == aktualneWersje.getOrDefault(w.getDokument().getId(), -1))
                .collect(Collectors.toMap(
                        w -> w.getDokument().getId(),
                        w -> DokumentyMapper.statusPodpisuFromDb(w.getStatusPodpisu()),
                        (a, b) -> a));
    }

    public Dokument getById(String id) {
        return DokumentyMapper.toDto(getEntityById(id));
    }

    private DokumentEntity getEntityById(String id) {
        return dokumentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Dokument o id=" + id + " nie istnieje"));
    }

    public List<WersjaDokumentu> listaWersji(String dokumentId) {
        // 404 jeśli dokument nie istnieje, zamiast cichej pustej listy.
        getEntityById(dokumentId);
        return wersjaDokumentuRepository.findByDokument_IdOrderByWersjaDesc(dokumentId)
                .stream().map(DokumentyMapper::toDto).toList();
    }

    /**
     * Orkiestracja podpisania PAdES aktualnej wersji dokumentu: pobranie bajtów z dms_service,
     * podpisanie w sign_service, upload podpisanego wyniku jako NOWA wersja w dms_service, i
     * utworzenie odpowiadającego jej lokalnego wiersza wersja_dokumentu. Podpisywana jest zawsze
     * aktualnaWersja, nigdy konkretna wersja podana przez wołającego - podpisanie z definicji
     * tworzy nową wersję (dms_service.POST .../wersje zawsze przydziela MAX+1), więc "podpisz
     * wersję N" i "podpisz aktualną wersję, co da wersję N+1" to ta sama operacja; endpoint nie
     * przyjmuje numeru wersji, żeby nie sugerować możliwości podpisania wersji nieaktualnej.
     * XAdES jest poza zakresem - ten serwis ufa, że dokument jest plikiem PDF.
     * <p>
     * Jeśli aktualna wersja jest już podpisana, operacja jest odrzucana (BadRequestException) -
     * wersja podpisana jest niemutowalna (zob. main_assumptions.md), a ponowne podpisanie tych
     * samych bajtów nie ma sensu domenowego; żeby wprowadzić zmianę do podpisanego dokumentu,
     * trzeba najpierw utworzyć nową wersję roboczą (poza zakresem tego zadania - brak jeszcze
     * żadnego endpointu do tworzenia wersji roboczej spoza akcji zbiorowej generowania dokumentu).
     * <p>
     * Kolejność kroków: najpierw WSZYSTKIE wywołania HTTP (fetch -> sign -> upload), na końcu
     * zapis lokalny (nowy wiersz wersja_dokumentu + aktualizacja dokument.aktualna_wersja + wpis
     * audytu) - jeśli HTTP zawiedzie, transakcja lokalna nigdy się nie zaczyna. Odwrotnie: jeśli
     * lokalny zapis zawiedzie PO udanym uploadzie do dms_service, nowa wersja zostaje osierocona
     * w dms_service (istnieje tam, ale nie ma odpowiadającego wiersza w core.wersja_dokumentu) -
     * bez mechanizmu kompensacji w tym zadaniu (poza zakresem; do rozważenia przy przyszłej pracy
     * nad odpornością orkiestracji wieloserwisowej).
     */
    @Transactional
    public WersjaDokumentu podpiszDokument(String dokumentId, String certyfikatId, String osoba) {
        DokumentEntity dokument = getEntityById(dokumentId);
        WersjaDokumentuEntity aktualnaWersja = wersjaDokumentuRepository
                .findByDokument_IdAndWersja(dokumentId, dokument.getAktualnaWersja())
                .orElseThrow(() -> new BadRequestException(
                        "Dokument o id=" + dokumentId + " nie ma wiersza wersja_dokumentu dla aktualnej wersji "
                                + dokument.getAktualnaWersja() + " - nie można podpisać"));
        if (aktualnaWersja.isPodpisana()) {
            throw new BadRequestException(
                    "Aktualna wersja (" + aktualnaWersja.getWersja() + ") dokumentu o id=" + dokumentId
                            + " jest już podpisana - wersja podpisana jest niemutowalna");
        }

        byte[] plikDoPodpisania = dmsServiceClient.pobierzPlikWersji(dokumentId, aktualnaWersja.getWersja());

        // Id nowego wiersza wersja_dokumentu generowane PRZED podpisaniem, żeby przekazać je jako
        // dokumentReferencja do sign_service - zgodnie z kontraktem to płaska referencja do
        // core.wersja_dokumentu.id NOWEJ (podpisanej) wersji, nie wersji źródłowej.
        String nowaWersjaId = UUID.randomUUID().toString();
        SignServiceClient.WynikPodpisaniaPades wynikPodpisania =
                signServiceClient.podpiszPades(plikDoPodpisania, certyfikatId, nowaWersjaId);

        int nowyNumerWersji = dmsServiceClient.uploadNowaWersja(
                dokumentId, osoba, wynikPodpisania.podpisanyPlik(), dokument.getNazwa() + ".pdf");

        WersjaDokumentuEntity nowaWersja = new WersjaDokumentuEntity();
        nowaWersja.setId(nowaWersjaId);
        nowaWersja.setDokument(dokument);
        nowaWersja.setWersja(nowyNumerWersji);
        nowaWersja.setPodpisana(true);
        nowaWersja.setDataUtworzenia(LocalDate.now());
        nowaWersja.setAutor(osoba);
        // sign_service.podpiszPades gwarantuje sukces tylko z certyfikatem aktywnym i
        // nie-wygasłym (inaczej 400 Bad Request bez złożenia podpisu) - świeżo złożony podpis
        // jest więc bezpiecznie "poprawny" bez dodatkowego wywołania weryfikacji.
        nowaWersja.setStatusPodpisu(StatusPodpisu.POPRAWNY.getWartosc());
        wersjaDokumentuRepository.save(nowaWersja);

        dokument.setAktualnaWersja(nowyNumerWersji);
        dokumentRepository.save(dokument);

        auditLogService.zarejestrujZdarzenie(TypOperacjiAudytu.PODPISANIE_DOKUMENTU, osoba, dokumentId,
                "Podpisano wersję " + nowyNumerWersji + " dokumentu " + dokumentId
                        + " certyfikatem " + certyfikatId + " (wystawiony_podpis_id="
                        + wynikPodpisania.wystawionyPodpisId() + ")");

        return DokumentyMapper.toDto(nowaWersja);
    }
}
