package robert_neat.document_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.client.SignServiceClient;
import robert_neat.document_api.dto.dokumenty.Certyfikat;

import java.util.List;

/** Proxy do sign_service GET /api/certyfikaty - cert-picker przy podpisywaniu dokumentu
 * (DokumentController.podpiszDokument). Zwraca WSZYSTKIE certyfikaty, bez filtrowania po
 * tożsamości użytkownika - w systemie nie istnieje model użytkownik↔certyfikat (certyfikat.podmiot
 * to DN X.509, nie login), zob. plan.md i main_assumptions.md. */
@RestController
@RequestMapping("/api/certyfikaty")
public class CertyfikatController {

    private final SignServiceClient signServiceClient;

    public CertyfikatController(SignServiceClient signServiceClient) {
        this.signServiceClient = signServiceClient;
    }

    @GetMapping
    public List<Certyfikat> listaCertyfikatow() {
        return signServiceClient.listaCertyfikatow();
    }
}
