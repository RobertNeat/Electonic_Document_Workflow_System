package robert_neat.delivery_mock.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import robert_neat.delivery_mock.dto.PrzesylkaDostawy;
import robert_neat.delivery_mock.dto.PrzesylkaDostawyZHistoria;
import robert_neat.delivery_mock.dto.UtworzPrzesylkeDostawyRequest;
import robert_neat.delivery_mock.mapper.DeliveryMapper;
import robert_neat.delivery_mock.service.PrzesylkaDostawyService;

import java.util.List;

/** REST API delivery_mock (dla symetrii z resztą systemu / ewentualnych testów-integracji) -
 * nie konsumowane jeszcze przez żaden inny moduł, zob. contracts/delivery_mock.md. */
@RestController
@RequestMapping("/api/przesylki-dostawy")
public class PrzesylkaDostawyController {

    private final PrzesylkaDostawyService service;

    public PrzesylkaDostawyController(PrzesylkaDostawyService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrzesylkaDostawy> listaWszystkich() {
        return service.listaWszystkich().stream().map(DeliveryMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    public PrzesylkaDostawyZHistoria getById(@PathVariable String id) {
        return service.getSzczegoly(id);
    }

    @PostMapping
    public ResponseEntity<PrzesylkaDostawy> rejestrujPrzesylke(@Valid @RequestBody UtworzPrzesylkeDostawyRequest request) {
        var entity = service.rejestrujPrzesylke(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(DeliveryMapper.toDto(entity));
    }
}
