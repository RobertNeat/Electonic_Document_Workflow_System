package robert_neat.dms_service.exception;

/** Rzucane, gdy żądany zasób (szablon, wersja dokumentu, plik) nie istnieje. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
