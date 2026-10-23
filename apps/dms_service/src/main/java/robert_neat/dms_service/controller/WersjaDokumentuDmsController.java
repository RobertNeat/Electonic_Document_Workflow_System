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
import robert_neat.dms_service.dto.WersjaDokumentuDms;
import robert_neat.dms_service.entity.WersjaDokumentuDmsEntity;
import robert_neat.dms_service.mapper.DmsMapper;
import robert_neat.dms_service.service.WersjaDokumentuDmsService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/dokumenty/{dokumentId}/wersje")
public class WersjaDokumentuDmsController {

    private final WersjaDokumentuDmsService service;

    public WersjaDokumentuDmsController(WersjaDokumentuDmsService service) {
        this.service = service;
    }

    @GetMapping
    public List<WersjaDokumentuDms> listaWersji(@PathVariable String dokumentId) {
        return service.listaWersji(dokumentId).stream().map(DmsMapper::toDto).toList();
    }

    @GetMapping("/{wersja}")
    public WersjaDokumentuDms getWersja(@PathVariable String dokumentId, @PathVariable int wersja) {
        return DmsMapper.toDto(service.getWersja(dokumentId, wersja));
    }

    // autor przyjmowany jako parametr formularza - brak jeszcze integracji JWT/Keycloak w tym serwisie.
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<WersjaDokumentuDms> uploadWersja(
            @PathVariable String dokumentId,
            @RequestParam String autor,
            @RequestPart("plik") MultipartFile plik) {
        WersjaDokumentuDmsEntity entity = service.uploadWersja(dokumentId, autor, plik);
        return ResponseEntity.status(HttpStatus.CREATED).body(DmsMapper.toDto(entity));
    }

    @GetMapping("/{wersja}/plik")
    public ResponseEntity<Resource> pobierzPlik(@PathVariable String dokumentId, @PathVariable int wersja) {
        WersjaDokumentuDmsEntity entity = service.getWersja(dokumentId, wersja);
        Resource resource = service.wczytajPlik(dokumentId, wersja);
        String nazwaPliku = Path.of(entity.getSciezkaPliku()).getFileName().toString();
        MediaType mediaType = MediaTypeFactory.getMediaType(nazwaPliku).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(nazwaPliku, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
