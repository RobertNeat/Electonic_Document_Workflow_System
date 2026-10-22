package robert_neat.document_api.dto.akcjezbiorowe;

import java.util.List;

public record StatusAkcjiZbiorowej(
        String akcjaId,
        TypAkcjiZbiorowej typAkcji,
        TypCeluAkcjiZbiorowej typCelu,
        StatusWykonaniaAkcji status,
        int postep,
        int lacznie,
        int przetworzone,
        int bledne,
        List<WynikElementuAkcji> wynikiElementow
) {
}
