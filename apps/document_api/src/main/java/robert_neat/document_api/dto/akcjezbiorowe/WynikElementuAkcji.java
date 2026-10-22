package robert_neat.document_api.dto.akcjezbiorowe;

public record WynikElementuAkcji(
        String idCelu,
        StatusElementuAkcji status,
        String komunikat
) {
}
