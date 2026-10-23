package robert_neat.document_api.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import robert_neat.document_api.config.RabbitMqConfig;
import robert_neat.document_api.dto.akcjezbiorowe.AkcjaZbiorowaMessage;
import robert_neat.document_api.dto.akcjezbiorowe.StatusAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.TypAkcjiZbiorowej;
import robert_neat.document_api.dto.akcjezbiorowe.UruchomAkcjeZbiorowaRequest;
import robert_neat.document_api.dto.akcjezbiorowe.UruchomAkcjeZbiorowaResponse;
import robert_neat.document_api.entity.AkcjaZbiorowaElementEntity;
import robert_neat.document_api.entity.AkcjaZbiorowaEntity;
import robert_neat.document_api.exception.NotFoundException;
import robert_neat.document_api.mapper.AkcjeZbioroweMapper;
import robert_neat.document_api.repository.AkcjaZbiorowaElementRepository;
import robert_neat.document_api.repository.AkcjaZbiorowaRepository;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Strona "producenta/odpytywania" akcji zbiorowych - utworzenie+publikacja do RabbitMQ oraz
 * odczyt statusu. Przetwarzanie per-element żyje w AkcjaZbiorowaProcessingService (wołane przez
 * AkcjaZbiorowaConsumer), żeby rozdzielić transakcje per-item od pojedynczej transakcji "uruchom". */
@Service
@Transactional(readOnly = true)
public class AkcjeZbioroweService {

    private final AkcjaZbiorowaRepository akcjaZbiorowaRepository;
    private final AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public AkcjeZbioroweService(AkcjaZbiorowaRepository akcjaZbiorowaRepository,
                                 AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository,
                                 RabbitTemplate rabbitTemplate,
                                 JsonMapper jsonMapper) {
        this.akcjaZbiorowaRepository = akcjaZbiorowaRepository;
        this.akcjaZbiorowaElementRepository = akcjaZbiorowaElementRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    /** Zapis encji + elementów commituje się w tej metodzie; publikacja do RabbitMQ następuje
     * dopiero PO commicie (TransactionalEventListener(AFTER_COMMIT) niżej) - inaczej konsument
     * mógłby odebrać wiadomość i nie znaleźć jeszcze zacommitowanego wiersza (RabbitTemplate nie
     * jest domyślnie transakcyjny, więc convertAndSend() wołane wewnątrz tej metody wysłałby
     * wiadomość natychmiast, przed commitem). osoba - "zleceniodawca" rozwiązany z JWT w kontrolerze
     * (wątek konsumenta RabbitMQ nie ma SecurityContext), przenoszony w wiadomości do użycia jako
     * autor wpisów metryki / nowej wersji dokumentu tam, gdzie dany typ akcji tego wymaga. */
    @Transactional
    public UruchomAkcjeZbiorowaResponse uruchom(UruchomAkcjeZbiorowaRequest request, String osoba) {
        if (request.typAkcji() == TypAkcjiZbiorowej.AKTUALIZACJA_ROL && !maRoleAdministratora()) {
            throw new AccessDeniedException(
                    "Akcja zbiorowa AKTUALIZACJA_ROL wymaga roli administrator - PUT /api/uzytkownicy/{id}/role "
                            + "jest administrator-only, ta akcja nie może obchodzić tej reguły");
        }

        List<String> idCelow = request.idCelow() == null ? List.of() : request.idCelow();

        AkcjaZbiorowaEntity akcja = new AkcjaZbiorowaEntity();
        akcja.setId(UUID.randomUUID().toString());
        akcja.setTypAkcji(AkcjeZbioroweMapper.typAkcjiToDb(request.typAkcji()));
        akcja.setTypCelu(AkcjeZbioroweMapper.typCeluToDb(request.typCelu()));
        akcja.setKryteria(request.kryteria() == null ? null : jsonMapper.writeValueAsString(request.kryteria()));
        akcja.setParametry(request.parametry() == null ? "{}" : jsonMapper.writeValueAsString(request.parametry()));
        akcja.setStatus("trwajaca");
        akcja.setPostep(idCelow.isEmpty() ? 100 : 0);
        akcja.setLacznie(idCelow.size());
        akcja.setPrzetworzone(0);
        akcja.setBledne(0);
        akcja.setDataUtworzenia(OffsetDateTime.now());
        akcjaZbiorowaRepository.save(akcja);

        for (String idCelu : idCelow) {
            AkcjaZbiorowaElementEntity element = new AkcjaZbiorowaElementEntity();
            element.setId(UUID.randomUUID().toString());
            element.setAkcjaZbiorowa(akcja);
            element.setIdCelu(idCelu);
            akcjaZbiorowaElementRepository.save(element);
        }

        if (idCelow.isEmpty()) {
            // Nic do przetworzenia - akcja kończy się natychmiast, bez publikacji do RabbitMQ.
            akcja.setStatus("zakonczona");
            akcjaZbiorowaRepository.save(akcja);
        } else {
            publikujPoKomicie(new AkcjaZbiorowaMessage(akcja.getId(), osoba));
        }

        return new UruchomAkcjeZbiorowaResponse(akcja.getId());
    }

    /** Rejestruje publikację do RabbitMQ jako synchronizację AFTER_COMMIT bieżącej transakcji -
     * RabbitTemplate.convertAndSend nie jest domyślnie transakcyjne, więc wołane wprost tutaj
     * wysłałoby wiadomość natychmiast, przed zacommitowaniem wiersza akcja_zbiorowa (wyścig
     * z konsumentem, który robi findById). */
    private void publikujPoKomicie(AkcjaZbiorowaMessage message) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, message);
            }
        });
    }

    /** AKTUALIZACJA_ROL zmienia role domenowe (core) użytkownika - ta sama funkcja co
     * PUT /api/uzytkownicy/{id}/role (administrator-only w SecurityConfig). requestMatchers po
     * ścieżce+metodzie nie widzi typAkcji w body generycznego POST /api/akcje-zbiorowe, więc
     * kontrola musi być tutaj, przed utworzeniem/opublikowaniem akcji. */
    private boolean maRoleAdministratora() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_administrator".equals(authority.getAuthority()));
    }

    public StatusAkcjiZbiorowej pobierzStatus(String akcjaId) {
        AkcjaZbiorowaEntity akcja = akcjaZbiorowaRepository.findById(akcjaId)
                .orElseThrow(() -> new NotFoundException("Akcja zbiorowa o id=" + akcjaId + " nie istnieje"));
        List<AkcjaZbiorowaElementEntity> elementy = akcjaZbiorowaElementRepository.findByAkcjaZbiorowa_Id(akcjaId);
        return AkcjeZbioroweMapper.toDto(akcja, elementy);
    }
}
