package robert_neat.document_api.exception;

/** Rzucane, gdy żądana zmiana stanu sprawy nie jest dozwolonym przejściem wg definicji obiegu. */
public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
