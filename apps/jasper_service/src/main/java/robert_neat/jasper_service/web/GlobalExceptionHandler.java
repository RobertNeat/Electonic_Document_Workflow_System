package robert_neat.jasper_service.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import robert_neat.jasper_service.exception.BadRequestException;
import robert_neat.jasper_service.exception.NotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimalny, reużywalny handler błędów - analogiczny do document_api/dms_service. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("komunikat", ex.getMessage()));
    }

    // Logowane na WARN z przyczyną (cause), bo BadRequestException zgłaszany przez
    // RenderowanieService opakowuje błędy JasperReports (np. kompilacji .jrxml), których treść w
    // samym komunikacie bywa zbyt ogólna, by zdiagnozować problem bez stack trace.
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
        log.warn("Żądanie odrzucone: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("komunikat", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> bledyPol = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            bledyPol.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("komunikat", "Błąd walidacji danych wejściowych");
        body.put("bledyPol", bledyPol);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
