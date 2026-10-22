package robert_neat.document_api.dto.akcjezbiorowe;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * parametry: konkretna postać zależy od typAkcji (zob. Parametry* w tym pakiecie) — dekodowanie
 * i walidacja zgodności typAkcji/parametry wykracza poza zakres tego zadania (tylko DTO).
 */
public record UruchomAkcjeZbiorowaRequest(
        @NotNull TypAkcjiZbiorowej typAkcji,
        @NotNull TypCeluAkcjiZbiorowej typCelu,
        List<String> idCelow,
        Map<String, Object> kryteria,
        Object parametry
) {
}
