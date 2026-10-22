package robert_neat.document_api.dto.wymiana;

import java.util.List;

public record WynikWalidacjiUbl(
        boolean sukces,
        List<BladWalidacjiXsd> bledy,
        FakturaUbl faktura
) {
}
