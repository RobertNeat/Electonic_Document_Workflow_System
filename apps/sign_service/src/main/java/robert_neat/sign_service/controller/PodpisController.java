package robert_neat.sign_service.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.sign_service.dto.WynikWeryfikacji;
import robert_neat.sign_service.exception.BadRequestException;
import robert_neat.sign_service.service.PodpisService;
import robert_neat.sign_service.service.WeryfikacjaService;

import java.io.IOException;
import java.io.UncheckedIOException;

@RestController
@RequestMapping("/api/podpisy")
public class PodpisController {

    private static final String HEADER_WYSTAWIONY_PODPIS_ID = "X-Wystawiony-Podpis-Id";

    private final PodpisService podpisService;
    private final WeryfikacjaService weryfikacjaService;

    public PodpisController(PodpisService podpisService, WeryfikacjaService weryfikacjaService) {
        this.podpisService = podpisService;
        this.weryfikacjaService = weryfikacjaService;
    }

    @PostMapping(value = "/pades", consumes = "multipart/form-data")
    public ResponseEntity<byte[]> podpiszPades(
            @RequestPart("plik") MultipartFile plik,
            @RequestParam String certyfikatId,
            @RequestParam String dokumentReferencja) {
        byte[] bajty = readBytes(plik);
        PodpisService.WynikPodpisania wynik = podpisService.podpiszPades(bajty, certyfikatId, dokumentReferencja);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HEADER_WYSTAWIONY_PODPIS_ID, wynik.wystawionyPodpisId())
                .body(wynik.podpisanyPlik());
    }

    @PostMapping(value = "/weryfikacja", consumes = "multipart/form-data")
    public ResponseEntity<WynikWeryfikacji> weryfikujPades(@RequestPart("plik") MultipartFile plik) {
        byte[] bajty = readBytes(plik);
        return ResponseEntity.ok(weryfikacjaService.weryfikujPades(bajty));
    }

    private static byte[] readBytes(MultipartFile plik) {
        if (plik == null || plik.isEmpty()) {
            throw new BadRequestException("Plik jest wymagany");
        }
        try {
            return plik.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
