package robert_neat.sign_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.sign_service.dto.CertyfikatDto;
import robert_neat.sign_service.mapper.PkiMapper;
import robert_neat.sign_service.service.CertyfikatService;

import java.util.List;

@RestController
@RequestMapping("/api/certyfikaty")
public class CertyfikatController {

    private final CertyfikatService certyfikatService;

    public CertyfikatController(CertyfikatService certyfikatService) {
        this.certyfikatService = certyfikatService;
    }

    @GetMapping
    public List<CertyfikatDto> getAll() {
        return certyfikatService.getAll().stream().map(PkiMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public CertyfikatDto getById(@PathVariable String id) {
        return PkiMapper.toDto(certyfikatService.getById(id));
    }
}
