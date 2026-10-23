package robert_neat.dms_service.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import robert_neat.dms_service.dto.SzablonJasper;
import robert_neat.dms_service.entity.SzablonJasperEntity;
import robert_neat.dms_service.mapper.DmsMapper;
import robert_neat.dms_service.service.SzablonJasperService;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/szablony")
public class SzablonJasperController {

    private final SzablonJasperService service;

    public SzablonJasperController(SzablonJasperService service) {
        this.service = service;
    }

    @GetMapping
    public List<SzablonJasper> listaSzablonow() {
        return service.listaSzablonow().stream().map(DmsMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public SzablonJasper getById(@PathVariable String id) {
        return DmsMapper.toDto(service.getById(id));
    }

    // autor przyjmowany jako parametr formularza - brak jeszcze integracji JWT/Keycloak w tym serwisie.
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<SzablonJasper> uploadSzablon(
            @RequestParam String typDokumentuId,
            @RequestParam String nazwa,
            @RequestParam String autor,
            @RequestPart("plik") MultipartFile plik) {
        SzablonJasperEntity entity = service.uploadSzablon(typDokumentuId, nazwa, autor, plik);
        return ResponseEntity.status(HttpStatus.CREATED).body(DmsMapper.toDto(entity));
    }

    @GetMapping("/{id}/plik")
    public ResponseEntity<Resource> pobierzPlik(@PathVariable String id) {
        SzablonJasperEntity szablon = service.getById(id);
        Resource resource = service.wczytajPlik(id);
        MediaType mediaType = MediaTypeFactory.getMediaType(szablon.getNazwa()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(szablon.getNazwa(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
