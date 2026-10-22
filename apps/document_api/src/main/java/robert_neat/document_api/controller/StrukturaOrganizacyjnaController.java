package robert_neat.document_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.administracja.Wydzial;
import robert_neat.document_api.service.StrukturaOrganizacyjnaService;

import java.util.List;

/** Tylko podgląd - zarządzanie wydziałami (POST/PUT) poza zakresem obecnego frontendu
 * (zob. kontrakt administracja.md, sekcja "Struktura organizacyjna"). */
@RestController
@RequestMapping("/api")
public class StrukturaOrganizacyjnaController {

    private final StrukturaOrganizacyjnaService strukturaOrganizacyjnaService;

    public StrukturaOrganizacyjnaController(StrukturaOrganizacyjnaService strukturaOrganizacyjnaService) {
        this.strukturaOrganizacyjnaService = strukturaOrganizacyjnaService;
    }

    @GetMapping("/struktura-organizacyjna")
    public List<Wydzial> lista() {
        return strukturaOrganizacyjnaService.lista();
    }
}
