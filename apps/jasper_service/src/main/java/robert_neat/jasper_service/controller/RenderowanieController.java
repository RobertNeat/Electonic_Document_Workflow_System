package robert_neat.jasper_service.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.jasper_service.service.RenderowanieService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/render")
public class RenderowanieController {

    private final RenderowanieService renderowanieService;

    public RenderowanieController(RenderowanieService renderowanieService) {
        this.renderowanieService = renderowanieService;
    }

    @PostMapping(value = "/{szablonId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> renderuj(@PathVariable String szablonId, @RequestBody(required = false) Map<String, Object> dane) {
        Map<String, Object> parametry = dane != null ? dane : Map.of();
        RenderowanieService.WynikRenderowania wynik = renderowanieService.renderujPdf(szablonId, parametry);

        String nazwaBezRozszerzenia = usunRozszerzenie(wynik.nazwaSzablonu());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(nazwaBezRozszerzenia + ".pdf", StandardCharsets.UTF_8)
                .build();

        Resource resource = new ByteArrayResource(wynik.pdf());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    private String usunRozszerzenie(String nazwa) {
        int idx = nazwa.lastIndexOf('.');
        return idx > 0 ? nazwa.substring(0, idx) : nazwa;
    }
}
