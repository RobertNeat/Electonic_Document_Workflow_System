package robert_neat.dms_service.exception;

/** Rzucane dla danych wejściowych niedopuszczalnych semantycznie lub ze względów bezpieczeństwa
 * (np. nazwa pliku/identyfikator próbujący wyjść poza katalog bazowy przechowywania - path traversal). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
