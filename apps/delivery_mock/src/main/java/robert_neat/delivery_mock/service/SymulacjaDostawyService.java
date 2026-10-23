package robert_neat.delivery_mock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import robert_neat.delivery_mock.client.DocumentApiClient;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.exception.BadRequestException;

import java.util.Optional;

/** Logika symulacji zmiany statusu z panelu Thymeleaf. delivery_mock jest "źródłem prawdy" dla
 * statusu doręczenia - document_api go tylko odzwierciedla, więc awaria/niedostępność
 * document_api NIE wycofuje lokalnej zmiany (zob. main_assumptions.md "delivery_mock"). Zapis
 * lokalny (transakcyjny, w ZmianaStatusuDostawyWriter) jest wykonywany i zatwierdzany PRZED
 * wywołaniem HTTP do document_api (poza jakąkolwiek transakcją) - inaczej wyjątek z klienta HTTP
 * wycofałby też lokalny zapis. */
@Service
public class SymulacjaDostawyService {

    private static final Logger log = LoggerFactory.getLogger(SymulacjaDostawyService.class);

    private final ZmianaStatusuDostawyWriter writer;
    private final DocumentApiClient documentApiClient;

    public SymulacjaDostawyService(ZmianaStatusuDostawyWriter writer, DocumentApiClient documentApiClient) {
        this.writer = writer;
        this.documentApiClient = documentApiClient;
    }

    /** @return null jeśli synchronizacja z document_api się powiodła, albo komunikat
     * ostrzegawczy (do wyświetlenia na stronie) jeśli się nie powiodła - lokalna zmiana jest
     * zapisana niezależnie od wyniku synchronizacji. */
    public String symulujZmianeStatusu(String przesylkaDostawyId, String nowyStatus, String szczegoly) {
        if (!PrzesylkaDostawyService.STATUSY.contains(nowyStatus)) {
            throw new BadRequestException("Nieznany status: " + nowyStatus);
        }

        PrzesylkaDostawyEntity przesylka = writer.zapisz(przesylkaDostawyId, nowyStatus, szczegoly);

        try {
            Optional<String> idWDocumentApi = documentApiClient.znajdzIdPoNumerzeReferencyjnym(przesylka.getNumerReferencyjny());
            if (idWDocumentApi.isEmpty()) {
                String ostrzezenie = "Nie znaleziono przesyłki o numerze referencyjnym "
                        + przesylka.getNumerReferencyjny() + " w document_api - synchronizacja statusu pominięta.";
                log.warn(ostrzezenie);
                return ostrzezenie;
            }
            documentApiClient.aktualizujStatus(idWDocumentApi.get(), nowyStatus);
            return null;
        } catch (RuntimeException ex) {
            String ostrzezenie = "Synchronizacja statusu z document_api nie powiodła się: " + ex.getMessage()
                    + " - zmiana lokalna została zapisana.";
            log.warn(ostrzezenie, ex);
            return ostrzezenie;
        }
    }
}
