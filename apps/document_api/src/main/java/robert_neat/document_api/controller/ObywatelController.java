package robert_neat.document_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.document_api.dto.obywatele.Obywatel;
import robert_neat.document_api.service.ObywatelService;

import java.util.List;

@RestController
@RequestMapping("/api/obywatele")
public class ObywatelController {

    private final ObywatelService obywatelService;

    public ObywatelController(ObywatelService obywatelService) {
        this.obywatelService = obywatelService;
    }

    @GetMapping
    public List<Obywatel> search(@RequestParam(name = "query", required = false) String query) {
        return obywatelService.search(query);
    }

    @GetMapping("/{id}")
    public Obywatel getById(@PathVariable String id) {
        return obywatelService.getById(id);
    }
}
