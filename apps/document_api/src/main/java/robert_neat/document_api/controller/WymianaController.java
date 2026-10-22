package robert_neat.document_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.document_api.dto.wymiana.WpisHistoriiWymiany;
import robert_neat.document_api.dto.wymiana.WynikWalidacjiUbl;
import robert_neat.document_api.dto.wymiana.WynikWalidacjiXml;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.service.WymianaService;

import java.io.IOException;
import java.util.List;

/** Import/walidacja komunikatów sprawy XML i faktur UBL 2.1 (PEPPOL BIS Billing 3.0) + historia wymiany.
 * Akcje zbiorowe (RabbitMQ) poza zakresem - wymagają infrastruktury konsumenta, jeszcze nie istniejącej. */
@RestController
@RequestMapping("/api/wymiana")
public class WymianaController {

    private final WymianaService wymianaService;

    public WymianaController(WymianaService wymianaService) {
        this.wymianaService = wymianaService;
    }

    @GetMapping("/xml/historia")
    public List<WpisHistoriiWymiany> historiaXml() {
        return wymianaService.listaHistoriiXml();
    }

    @GetMapping("/ubl/historia")
    public List<WpisHistoriiWymiany> historiaUbl() {
        return wymianaService.listaHistoriiUbl();
    }

    /** Parametr "plik" zgodny z konwencją PlatnosciController./api/import-camt053. */
    @PostMapping("/xml")
    public WynikWalidacjiXml importujXml(@RequestParam("plik") MultipartFile plik) {
        walidujMultipartXml(plik);
        try {
            return wymianaService.importujXml(nazwaPliku(plik), plik.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Nie udało się odczytać przesłanego pliku: " + e.getMessage());
        }
    }

    @PostMapping("/ubl")
    public WynikWalidacjiUbl importujUbl(@RequestParam("plik") MultipartFile plik) {
        walidujMultipartXml(plik);
        try {
            return wymianaService.importujUbl(nazwaPliku(plik), plik.getInputStream());
        } catch (IOException e) {
            throw new BadRequestException("Nie udało się odczytać przesłanego pliku: " + e.getMessage());
        }
    }

    /** Nazwa pliku jest zapisywana w kolumnie NOT NULL varchar(256) - obrona przed brakującą nazwą
     * (klient nie zawsze ją wysyła) i przed przekroczeniem limitu długości kolumny. */
    private String nazwaPliku(MultipartFile plik) {
        String nazwa = plik.getOriginalFilename();
        if (nazwa == null || nazwa.isBlank()) {
            nazwa = "nieznany.xml";
        }
        return nazwa.length() > 256 ? nazwa.substring(0, 256) : nazwa;
    }

    private void walidujMultipartXml(MultipartFile plik) {
        if (plik.isEmpty()) {
            throw new BadRequestException("Przesłany plik jest pusty");
        }
        String contentType = plik.getContentType();
        boolean wygladaJakXml = (contentType != null && contentType.contains("xml"))
                || (plik.getOriginalFilename() != null && plik.getOriginalFilename().toLowerCase().endsWith(".xml"));
        if (!wygladaJakXml) {
            throw new BadRequestException("Przesłany plik nie jest plikiem XML");
        }
    }
}
