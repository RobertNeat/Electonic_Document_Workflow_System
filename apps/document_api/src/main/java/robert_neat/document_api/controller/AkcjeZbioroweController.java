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
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.akcjezbiorowe.StatusAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.UruchomAkcjeZbiorowaRequest;
import robert_neat.document_api.dto.akcjezbiorowe.UruchomAkcjeZbiorowaResponse;
import robert_neat.document_api.service.AkcjeZbioroweService;

/** Jeden generyczny endpoint POST /api/akcje-zbiorowe (nie trzy osobne ścieżki per typAkcji z
 * wcześniejszego draftu kontraktu w document_api_wymiana.md) - UruchomAkcjeZbiorowaRequest ma
 * jeden dyskryminator typAkcji, więc jedna ścieżka + dispatch po stronie konsumenta lepiej
 * odpowiada rzeczywistemu kształtowi DTO. 202 Accepted - żądanie jest tylko publikowane do
 * RabbitMQ i przetwarzane asynchronicznie, nie przetworzone synchronicznie jak przy 200/201. */
@RestController
@RequestMapping("/api")
public class AkcjeZbioroweController {

    private final AkcjeZbioroweService akcjeZbioroweService;

    public AkcjeZbioroweController(AkcjeZbioroweService akcjeZbioroweService) {
        this.akcjeZbioroweService = akcjeZbioroweService;
    }

    @PostMapping("/akcje-zbiorowe")
    public ResponseEntity<UruchomAkcjeZbiorowaResponse> uruchom(@Valid @RequestBody UruchomAkcjeZbiorowaRequest request,
                                                                  @AuthenticationPrincipal Jwt jwt) {
        UruchomAkcjeZbiorowaResponse response = akcjeZbioroweService.uruchom(request, osobaFromJwt(jwt));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/akcje-zbiorowe/{akcjaId}/status")
    public StatusAkcjiZbiorowej pobierzStatus(@PathVariable String akcjaId) {
        return akcjeZbioroweService.pobierzStatus(akcjaId);
    }

    private String osobaFromJwt(Jwt jwt) {
        if (jwt == null) {
            return "system";
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return jwt.getSubject() != null ? jwt.getSubject() : "system";
    }
}
