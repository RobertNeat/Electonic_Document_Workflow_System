package robert_neat.delivery_mock.exception;

/** Rzucane, gdy żądana przesyłka dostawy nie istnieje. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
