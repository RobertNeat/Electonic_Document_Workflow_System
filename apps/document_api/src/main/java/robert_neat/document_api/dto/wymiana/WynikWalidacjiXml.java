package robert_neat.document_api.dto.wymiana;

import java.util.List;
import java.util.Map;

/** sprawaZmapowana odpowiada TS Partial&lt;Sprawa&gt; — luźno typowana mapa jako pragmatyczny wybór dla DTO. */
public record WynikWalidacjiXml(
        boolean sukces,
        List<BladWalidacjiXsd> bledy,
        Map<String, Object> sprawaZmapowana
) {
}
