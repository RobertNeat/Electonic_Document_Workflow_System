package robert_neat.document_api.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.document_api.dto.platnosci.DopasujWplateRequest;
import robert_neat.document_api.dto.platnosci.Naleznosc;
import robert_neat.document_api.dto.platnosci.NaleznoscListElement;
import robert_neat.document_api.dto.platnosci.Wplata;
import robert_neat.document_api.dto.platnosci.WynikImportuCamt053;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.service.PlatnosciService;

import java.io.IOException;
import java.util.List;

/** Rejestr należności/wpłat i import wyciągów CAMT.053. Generowanie wezwania do zapłaty
 * (POST /api/naleznosci/:id/wezwanie) poza zakresem - wymaga jasper_service, który jeszcze nie istnieje. */
@RestController
@RequestMapping("/api")
public class PlatnosciController {

    private final PlatnosciService platnosciService;

    public PlatnosciController(PlatnosciService platnosciService) {
        this.platnosciService = platnosciService;
    }

    @GetMapping("/naleznosci")
    public List<NaleznoscListElement> listaNaleznosci(@RequestParam(required = false) String status) {
        return platnosciService.listaNaleznosci(status);
    }

    @GetMapping("/naleznosci/{id}")
    public Naleznosc getById(@PathVariable String id) {
        return platnosciService.getById(id);
    }

    @GetMapping("/wplaty")
    public List<Wplata> listaWplat(@RequestParam(required = false) String naleznoscId) {
        return platnosciService.listaWplat(naleznoscId);
    }

    @PostMapping("/wplaty/{id}/dopasuj")
    public Wplata dopasujWplate(@PathVariable String id, @Valid @RequestBody DopasujWplateRequest request) {
        return platnosciService.dopasujWplate(id, request);
    }

    /** Parametr "plik" - nazwa świadomie opisowa (zamiast np. "file"), zgodnie z polskim
     * nazewnictwem reszty API; frontend modułu Płatności jeszcze nie jest podłączony do tego
     * endpointu (obecnie mockuje import w PlatnosciStore), więc nazwa nie jest jeszcze ustalona kontraktem. */
    @PostMapping("/import-camt053")
    public WynikImportuCamt053 importujCamt053(@RequestParam("plik") MultipartFile plik) {
        if (plik.isEmpty()) {
            throw new BadRequestException("Przesłany plik jest pusty");
        }
        String contentType = plik.getContentType();
        boolean wygladaJakXml = (contentType != null && (contentType.contains("xml")))
                || (plik.getOriginalFilename() != null && plik.getOriginalFilename().toLowerCase().endsWith(".xml"));
        if (!wygladaJakXml) {
            throw new BadRequestException("Przesłany plik nie jest plikiem XML");
        }
        try {
            return platnosciService.importujCamt053(plik.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Nie udało się odczytać przesłanego pliku: " + e.getMessage());
        }
    }
}
