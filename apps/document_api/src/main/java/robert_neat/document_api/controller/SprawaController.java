package robert_neat.document_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.administracja.EdytujDefinicjeObieguRequest;
import robert_neat.document_api.dto.sprawy.DefinicjaObiegu;
import robert_neat.document_api.dto.sprawy.Sprawa;
import robert_neat.document_api.dto.sprawy.UtworzSpraweRequest;
import robert_neat.document_api.dto.sprawy.WpisMetryki;
import robert_neat.document_api.dto.sprawy.ZmienStanSprawyRequest;
import robert_neat.document_api.dto.wspolne.StronaWynikow;
import robert_neat.document_api.service.SprawaService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SprawaController {

    private final SprawaService sprawaService;

    public SprawaController(SprawaService sprawaService) {
        this.sprawaService = sprawaService;
    }

    @GetMapping("/sprawy")
    public StronaWynikow<Sprawa> szukaj(
            @RequestParam(required = false) String stan,
            @RequestParam(required = false) String wydzial,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String obywatelId,
            @RequestParam(name = "strona", defaultValue = "0") int strona,
            @RequestParam(name = "rozmiarStrony", defaultValue = "20") int rozmiarStrony) {
        return sprawaService.szukaj(stan, wydzial, q, obywatelId, strona, rozmiarStrony);
    }

    @GetMapping("/sprawy/{id}")
    public Sprawa getById(@PathVariable String id) {
        return sprawaService.getById(id);
    }

    @PostMapping("/sprawy")
    public ResponseEntity<Sprawa> rejestrujSprawe(@Valid @RequestBody UtworzSpraweRequest request) {
        Sprawa sprawa = sprawaService.rejestrujSprawe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sprawa);
    }

    @PatchMapping("/sprawy/{id}/stan")
    public Sprawa zmienStan(@PathVariable String id,
                             @Valid @RequestBody ZmienStanSprawyRequest request,
                             @AuthenticationPrincipal Jwt jwt) {
        return sprawaService.zmienStan(id, request, osobaFromJwt(jwt));
    }

    @GetMapping("/sprawy/{id}/metryka")
    public List<WpisMetryki> metryka(@PathVariable String id) {
        return sprawaService.pobierzMetryke(id);
    }

    @GetMapping("/definicje-obiegow")
    public List<DefinicjaObiegu> definicjeObiegow() {
        return sprawaService.listaDefinicjiObiegow();
    }

    @PutMapping("/definicje-obiegow/{id}")
    public DefinicjaObiegu edytujDefinicjeObiegu(@PathVariable String id,
                                                  @Valid @RequestBody EdytujDefinicjeObieguRequest request) {
        return sprawaService.edytujDefinicjeObiegu(id, request);
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
