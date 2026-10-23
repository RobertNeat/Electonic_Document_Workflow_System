package robert_neat.delivery_mock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import robert_neat.delivery_mock.mapper.DeliveryMapper;
import robert_neat.delivery_mock.service.PrzesylkaDostawyService;
import robert_neat.delivery_mock.service.SymulacjaDostawyService;

/** Panel symulacji statusów przesyłek - klasyczny kontroler Spring MVC + Thymeleaf (zwraca nazwy
 * widoków, nie JSON). Odrębny od PrzesylkaDostawyController (REST API, punkt 2 zakresu). */
@Controller
public class PrzesylkiWidokController {

    private final PrzesylkaDostawyService przesylkaService;
    private final SymulacjaDostawyService symulacjaService;

    public PrzesylkiWidokController(PrzesylkaDostawyService przesylkaService, SymulacjaDostawyService symulacjaService) {
        this.przesylkaService = przesylkaService;
        this.symulacjaService = symulacjaService;
    }

    @GetMapping({"/", "/przesylki"})
    public String lista(Model model) {
        model.addAttribute("przesylki", przesylkaService.listaWszystkich().stream().map(DeliveryMapper::toDto).toList());
        return "przesylki-lista";
    }

    @GetMapping("/przesylki/{id}")
    public String szczegoly(@PathVariable String id, Model model) {
        var szczegoly = przesylkaService.getSzczegoly(id);
        model.addAttribute("przesylka", szczegoly.przesylka());
        model.addAttribute("historia", szczegoly.historiaStatusow());
        model.addAttribute("statusy", PrzesylkaDostawyService.STATUSY);
        return "przesylki-szczegoly";
    }

    @PostMapping("/przesylki/{id}/symuluj-status")
    public String symulujStatus(@PathVariable String id,
                                 @RequestParam String nowyStatus,
                                 @RequestParam(required = false) String szczegoly,
                                 RedirectAttributes redirectAttributes) {
        String ostrzezenie = symulacjaService.symulujZmianeStatusu(id, nowyStatus, szczegoly);
        if (ostrzezenie != null) {
            redirectAttributes.addFlashAttribute("ostrzezenieSynchronizacji", ostrzezenie);
        } else {
            redirectAttributes.addFlashAttribute("komunikat", "Status zmieniony na \"" + nowyStatus + "\" i zsynchronizowany z document_api.");
        }
        return "redirect:/przesylki/" + id;
    }
}
