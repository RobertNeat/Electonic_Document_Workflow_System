package robert_neat.document_api.dto.dokumenty;

import jakarta.validation.constraints.NotBlank;

/** Żądanie podpisania aktualnej wersji dokumentu. Bez numeru wersji - podpisywana jest zawsze
 * aktualnaWersja dokumentu, bo podpisanie i tak tworzy nową wersję (aktualnaWersja+1) w dms_service,
 * nigdy nie modyfikuje wersji istniejącej. */
public record PodpiszDokumentRequest(
        @NotBlank String certyfikatId
) {
}
