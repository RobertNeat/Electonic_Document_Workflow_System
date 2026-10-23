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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.korespondencja.Przesylka;
import robert_neat.document_api.dto.korespondencja.UtworzPrzesylkeRequest;
import robert_neat.document_api.service.KorespondencjaService;

import java.util.List;

/** Rejestr korespondencji (nadania/przesyłki). Aktualizacja statusu przesyłki NIE jest tu
 * wystawiona jako endpoint - odbywa się asynchronicznie, odbiorem zdarzenia RabbitMQ z
 * delivery_mock (zob. PrzesylkaStatusuConsumer). */
@RestController
@RequestMapping("/api")
public class PrzesylkaController {

    private final KorespondencjaService korespondencjaService;

    public PrzesylkaController(KorespondencjaService korespondencjaService) {
        this.korespondencjaService = korespondencjaService;
    }

    @GetMapping("/przesylki")
    public List<Przesylka> listaPrzesylek(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kanal,
            @RequestParam(required = false) String kierunek) {
        return korespondencjaService.listaPrzesylek(status, kanal, kierunek);
    }

    @GetMapping("/przesylki/{id}")
    public Przesylka getById(@PathVariable String id) {
        return korespondencjaService.getById(id);
    }

    @PostMapping("/przesylki")
    public ResponseEntity<Przesylka> nadajPismo(@Valid @RequestBody UtworzPrzesylkeRequest request,
                                                  @AuthenticationPrincipal Jwt jwt) {
        Przesylka przesylka = korespondencjaService.nadajPismo(request, JwtActorUtil.osobaFromJwt(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(przesylka);
    }
}
