package robert_neat.document_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.dokumenty.Dokument;
import robert_neat.document_api.dto.dokumenty.DokumentListElement;
import robert_neat.document_api.dto.dokumenty.SzablonJasper;
import robert_neat.document_api.dto.dokumenty.WersjaDokumentu;
import robert_neat.document_api.service.DokumentService;

import java.util.List;

/** Metadane dokumentów/szablonów. Treść plików (podgląd/pobranie) i podpis cyfrowy (PAdES) są
 * poza zakresem tego serwisu - zob. dms_service / sign_service (nie istnieją jeszcze). */
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
            @RequestParam(required = false) String typDokumentuId) {
        return dokumentService.listaDokumentow(sprawaId, typDokumentuId);
    }

    @GetMapping("/dokumenty/{id}")
    public Dokument getById(@PathVariable String id) {
        return dokumentService.getById(id);
    }

    @GetMapping("/dokumenty/{id}/wersje")
    public List<WersjaDokumentu> listaWersji(@PathVariable String id) {
        return dokumentService.listaWersji(id);
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
