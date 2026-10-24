package robert_neat.sign_service.revocation;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Responder OCSP (RFC 6960) dla certyfikatow leaf wydanych przez issuing-CA tego serwisu - zob.
 * OcspResponderService. Bez uwierzytelnienia (sign_service nie ma Spring Security na classpath,
 * konsystentnie z reszta serwisu) - OCSP jest z definicji publicznym protokolem.
 */
@RestController
public class OcspController {

    private final OcspResponderService ocspResponderService;

    public OcspController(OcspResponderService ocspResponderService) {
        this.ocspResponderService = ocspResponderService;
    }

    @PostMapping(value = "/api/ocsp", consumes = "application/ocsp-request", produces = "application/ocsp-response")
    public ResponseEntity<byte[]> handleOcspRequest(@RequestBody(required = false) byte[] requestDer) {
        byte[] responseDer = ocspResponderService.handle(requestDer != null ? requestDer : new byte[0]);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/ocsp-response"))
                .body(responseDer);
    }
}
