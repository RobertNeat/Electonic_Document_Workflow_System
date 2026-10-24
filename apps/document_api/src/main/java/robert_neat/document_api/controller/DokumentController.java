package robert_neat.document_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.dokumenty.Dokument;
import robert_neat.document_api.dto.dokumenty.DokumentListElement;
import robert_neat.document_api.dto.dokumenty.PodpiszDokumentRequest;
import robert_neat.document_api.dto.dokumenty.SzablonJasper;
import robert_neat.document_api.dto.dokumenty.WersjaDokumentu;
import robert_neat.document_api.service.DokumentService;

import java.util.List;

/** Metadane dokumentów/szablonów oraz orkiestracja podpisania PAdES (zob. podpiszDokument).
 * Podgląd/pobranie treści pliku bezpośrednio z dms_service i orkiestracja XAdES są poza
 * zakresem tego kontrolera (nie istnieją jeszcze - zob. plan.md). */
@RestController
@RequestMapping("/api")
public class DokumentController {

    private final DokumentService dokumentService;

    public DokumentController(DokumentService dokumentService) {
        this.dokumentService = dokumentService;
    }

    @GetMapping("/dokumenty")
    public List<DokumentListElement> listaDokumentow(
            @RequestParam(required = false) String sprawaId,
            @RequestParam(required = false) String typDokumentuId,
            @RequestParam(required = false) String obywatelId) {
        return dokumentService.listaDokumentow(sprawaId, typDokumentuId, obywatelId);
    }

    @GetMapping("/dokumenty/{id}")
    public Dokument getById(@PathVariable String id) {
        return dokumentService.getById(id);
    }

    @GetMapping("/dokumenty/{id}/wersje")
    public List<WersjaDokumentu> listaWersji(@PathVariable String id) {
        return dokumentService.listaWersji(id);
    }

    /** Podpisuje PAdES aktualną wersję dokumentu - zob. Javadoc DokumentService.podpiszDokument
     * dla uzasadnienia, czemu żądanie nie przyjmuje numeru wersji. Odpowiedź to NOWA wersja
     * (aktualnaWersja+1), nie ta, która została odczytana z dms_service. */
    @PostMapping("/dokumenty/{id}/podpisz")
    public ResponseEntity<WersjaDokumentu> podpiszDokument(@PathVariable String id,
                                                             @Valid @RequestBody PodpiszDokumentRequest request,
                                                             @AuthenticationPrincipal Jwt jwt) {
        WersjaDokumentu wersja = dokumentService.podpiszDokument(
                id, request.certyfikatId(), JwtActorUtil.osobaFromJwt(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(wersja);
    }

    @GetMapping("/szablony")
    public List<SzablonJasper> listaSzablonow(@RequestParam(required = false) String typDokumentuId) {
        return dokumentService.listaSzablonow(typDokumentuId);
    }

    @GetMapping("/szablony/{id}/historia")
    public List<SzablonJasper> historiaSzablonu(@PathVariable String id) {
        return dokumentService.historiaSzablonu(id);
    }
}
