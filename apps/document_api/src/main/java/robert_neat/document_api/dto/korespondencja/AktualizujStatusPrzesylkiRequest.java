package robert_neat.document_api.dto.korespondencja;

import jakarta.validation.constraints.NotNull;

/** DTO dodatkowe (nie część zamrożonego kontraktu dto.korespondencja) - wejście dla synchronicznej
 * aktualizacji statusu przesyłki POST /api/przesylki/{id}/status, zamiast webhooka z delivery_mock
 * (integracja RabbitMQ/delivery_mock jest poza zakresem tego etapu). */
public record AktualizujStatusPrzesylkiRequest(
        @NotNull StatusPrzesylki nowyStatus
) {
}
