package robert_neat.delivery_mock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.exception.BadRequestException;

/** Logika symulacji zmiany statusu z panelu Thymeleaf. delivery_mock jest "źródłem prawdy" dla
 * statusu doręczenia - document_api go tylko odzwierciedla, więc awaria/niedostępność RabbitMQ
 * NIE wycofuje lokalnej zmiany (zob. main_assumptions.md "delivery_mock"). Zapis lokalny
 * (transakcyjny, w ZmianaStatusuDostawyWriter) jest wykonywany i zatwierdzany PRZED publikacją
 * zdarzenia do RabbitMQ (poza jakąkolwiek transakcją) - inaczej wyjątek z publikacji wycofałby
 * też lokalny zapis. */
@Service
public class SymulacjaDostawyService {

    private static final Logger log = LoggerFactory.getLogger(SymulacjaDostawyService.class);

    private final ZmianaStatusuDostawyWriter writer;
    private final ZmianaStatusuPrzesylkiPublisher publisher;

    public SymulacjaDostawyService(ZmianaStatusuDostawyWriter writer, ZmianaStatusuPrzesylkiPublisher publisher) {
        this.writer = writer;
        this.publisher = publisher;
    }

    /** @return null jeśli publikacja zdarzenia do RabbitMQ się powiodła, albo komunikat
     * ostrzegawczy (do wyświetlenia na stronie) jeśli się nie powiodła - lokalna zmiana jest
     * zapisana niezależnie od wyniku publikacji. */
    public String symulujZmianeStatusu(String przesylkaDostawyId, String nowyStatus, String szczegoly) {
        if (!PrzesylkaDostawyService.STATUSY.contains(nowyStatus)) {
            throw new BadRequestException("Nieznany status: " + nowyStatus);
        }

        PrzesylkaDostawyEntity przesylka = writer.zapisz(przesylkaDostawyId, nowyStatus, szczegoly);

        try {
            publisher.publikuj(przesylka.getNumerReferencyjny(), nowyStatus);
            return null;
        } catch (RuntimeException ex) {
            String ostrzezenie = "Publikacja zdarzenia zmiany statusu do document_api nie powiodła się: "
                    + ex.getMessage() + " - zmiana lokalna została zapisana.";
            log.warn(ostrzezenie, ex);
            return ostrzezenie;
        }
    }
}
