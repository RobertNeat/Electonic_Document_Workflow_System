package robert_neat.document_api.exception;

/** Rzucane, gdy żądany zasób (np. sprawa, obywatel, definicja obiegu) nie istnieje. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
