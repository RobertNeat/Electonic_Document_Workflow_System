package robert_neat.document_api.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.administracja.DostawcaWysylki;
import robert_neat.document_api.dto.administracja.PozycjaJrwa;
import robert_neat.document_api.dto.administracja.StawkaOplaty;
import robert_neat.document_api.dto.dokumenty.TypDokumentu;
import robert_neat.document_api.service.SlownikiService;

import java.util.List;

/** Słowniki administracyjne (JRWA, typy dokumentów, dostawcy wysyłki, stawki opłat) - PUT przyjmuje
 * pełny obiekt zasobu (brak dedykowanych request-DTO w pakiecie dto.administracja); id/symbol ze
 * ścieżki jest rozstrzygający, treść z body jest ignorowana. */
@RestController
@RequestMapping("/api/slowniki")
public class SlownikiController {

    private final SlownikiService slownikiService;

    public SlownikiController(SlownikiService slownikiService) {
        this.slownikiService = slownikiService;
    }

    @GetMapping("/jrwa")
    public List<PozycjaJrwa> listaJrwa() {
        return slownikiService.listaJrwa();
    }

    @PutMapping("/jrwa/{symbol}")
    public PozycjaJrwa aktualizujJrwa(@PathVariable String symbol, @Valid @RequestBody PozycjaJrwa zmiany) {
        return slownikiService.aktualizujJrwa(symbol, zmiany);
    }

    @GetMapping("/typy-dokumentow")
    public List<TypDokumentu> listaTypowDokumentow() {
        return slownikiService.listaTypowDokumentow();
    }

    @PutMapping("/typy-dokumentow/{id}")
    public TypDokumentu aktualizujTypDokumentu(@PathVariable String id, @Valid @RequestBody TypDokumentu zmiany) {
        return slownikiService.aktualizujTypDokumentu(id, zmiany);
    }

    @GetMapping("/dostawcy-wysylki")
    public List<DostawcaWysylki> listaDostawcowWysylki() {
        return slownikiService.listaDostawcowWysylki();
    }

    @PutMapping("/dostawcy-wysylki/{id}")
    public DostawcaWysylki aktualizujDostawceWysylki(@PathVariable String id, @Valid @RequestBody DostawcaWysylki zmiany) {
        return slownikiService.aktualizujDostawceWysylki(id, zmiany);
    }

    @GetMapping("/stawki-oplat")
    public List<StawkaOplaty> listaStawekOplat() {
        return slownikiService.listaStawekOplat();
    }

    @PutMapping("/stawki-oplat/{id}")
    public StawkaOplaty aktualizujStawkeOplaty(@PathVariable String id, @Valid @RequestBody StawkaOplaty zmiany) {
        return slownikiService.aktualizujStawkeOplaty(id, zmiany);
    }
}
