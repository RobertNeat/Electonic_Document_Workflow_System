package robert_neat.delivery_mock.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import robert_neat.delivery_mock.exception.BadRequestException;
import robert_neat.delivery_mock.exception.NotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/** Analogiczny do robert_neat.dms_service.web.GlobalExceptionHandler. Obsługuje tylko kontroler
 * REST (@RestController) - kontroler MVC/Thymeleaf łapie te wyjątki sam i renderuje widok. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("komunikat", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
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
