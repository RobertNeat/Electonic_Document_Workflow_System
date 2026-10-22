package robert_neat.document_api.exception;

/** Rzucane dla danych wejściowych poprawnych składniowo, ale niedopuszczalnych semantycznie (np. nieznany wydział). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
