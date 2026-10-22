package robert_neat.document_api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.dto.dokumenty.Dokument;
import robert_neat.document_api.dto.dokumenty.DokumentListElement;
import robert_neat.document_api.dto.dokumenty.StatusPodpisu;
import robert_neat.document_api.dto.dokumenty.SzablonJasper;
import robert_neat.document_api.dto.dokumenty.TypDokumentu;
import robert_neat.document_api.dto.dokumenty.WersjaDokumentu;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.DokumentyMapper;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.SzablonJasperRepository;
import robert_neat.document_api.repository.TypDokumentuRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DokumentService {

    private final DokumentRepository dokumentRepository;
    private final WersjaDokumentuRepository wersjaDokumentuRepository;
    private final TypDokumentuRepository typDokumentuRepository;
    private final SzablonJasperRepository szablonJasperRepository;

    public DokumentService(DokumentRepository dokumentRepository,
                            WersjaDokumentuRepository wersjaDokumentuRepository,
                            TypDokumentuRepository typDokumentuRepository,
                            SzablonJasperRepository szablonJasperRepository) {
        this.dokumentRepository = dokumentRepository;
        this.wersjaDokumentuRepository = wersjaDokumentuRepository;
        this.typDokumentuRepository = typDokumentuRepository;
        this.szablonJasperRepository = szablonJasperRepository;
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
}
