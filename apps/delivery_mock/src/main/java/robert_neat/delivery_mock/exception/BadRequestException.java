package robert_neat.delivery_mock.exception;

/** Rzucane dla danych wejściowych niedopuszczalnych semantycznie (np. nieznany kanał/status,
 * zduplikowany numer referencyjny). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
