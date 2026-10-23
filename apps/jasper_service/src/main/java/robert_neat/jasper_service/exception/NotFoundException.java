package robert_neat.jasper_service.exception;

/** Rzucane, gdy żądany zasób (np. szablon Jasper w dms_service) nie istnieje. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
