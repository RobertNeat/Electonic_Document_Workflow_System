package robert_neat.sign_service.revocation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint CRL (CDP) dla certyfikatow leaf wydanych przez issuing-CA - zob. CrlResponderService.
 * Bez uwierzytelnienia, tak jak OcspController. */
@RestController
public class CrlController {

    private final CrlResponderService crlResponderService;

    public CrlController(CrlResponderService crlResponderService) {
        this.crlResponderService = crlResponderService;
    }

    @GetMapping(value = "/api/crl/issuing-ca.crl", produces = "application/pkix-crl")
    public ResponseEntity<byte[]> getCrl() {
        byte[] crlDer = crlResponderService.buildCrl();
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/pkix-crl"))
                .body(crlDer);
    }
}
