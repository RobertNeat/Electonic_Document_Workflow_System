package robert_neat.jasper_service.exception;

/** Rzucane dla danych wejściowych poprawnych składniowo, ale niedopuszczalnych semantycznie (np. niepoprawny szablon/dane renderowania). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
