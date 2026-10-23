package robert_neat.document_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.administracja.AktualizujRoleUzytkownikaRequest;
import robert_neat.document_api.dto.administracja.RolaUzytkownika;
import robert_neat.document_api.dto.administracja.UtworzUzytkownikaRequest;
import robert_neat.document_api.dto.administracja.Uzytkownik;
import robert_neat.document_api.service.UzytkownikService;

import java.util.List;

/** Profil użytkownika w `core` (wydział, stanowisko, role domenowe) - konta/uwierzytelnianie
 * prowadzi Keycloak, bez integracji z jego API administracyjnym w tym miejscu. */
@RestController
@RequestMapping("/api")
public class UzytkownikController {

    private final UzytkownikService uzytkownikService;

    public UzytkownikController(UzytkownikService uzytkownikService) {
        this.uzytkownikService = uzytkownikService;
    }

    @GetMapping("/uzytkownicy")
    public List<Uzytkownik> lista(@RequestParam(required = false) String wydzial,
                                   @RequestParam(required = false) RolaUzytkownika rola) {
        return uzytkownikService.lista(wydzial, rola);
    }

    @PostMapping("/uzytkownicy")
    public ResponseEntity<Uzytkownik> utworz(@Valid @RequestBody UtworzUzytkownikaRequest request) {
        Uzytkownik uzytkownik = uzytkownikService.utworz(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(uzytkownik);
    }

    @PutMapping("/uzytkownicy/{id}/role")
    public Uzytkownik aktualizujRole(@PathVariable String id,
                                      @Valid @RequestBody AktualizujRoleUzytkownikaRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return uzytkownikService.aktualizujRole(id, request, JwtActorUtil.osobaFromJwt(jwt));
    }
}
