package robert_neat.document_api.dto.administracja;

public record DostawcaWysylki(
        String id,
        String nazwa,
        TypDostawcyWysylki typ,
        String operatorPocztowy,
        String email,
        boolean aktywny
) {
}
