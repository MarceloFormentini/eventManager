
package br.com.formentini.eventManager.shared.exception;

import br.com.formentini.eventManager.event.EventScheduleConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            ResourceNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(EventScheduleConflictException.class)
    public ResponseEntity<Map<String, String>> handleEventScheduleConflict(
            EventScheduleConflictException exception) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(
            IllegalArgumentException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException exception) {

        Set<String> errors = new LinkedHashSet<>(exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> fieldNameInPortuguese(error.getField())
                        + ": " + error.getDefaultMessage())
                .toList());

        return ResponseEntity.badRequest()
                .body(Map.of("error", String.join("; ", errors)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "Os dados enviados são inválidos. Verifique os campos e valores informados."
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleInvalidParameter(
            MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "O parâmetro '" + exception.getName()
                                + "' possui um formato inválido."
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(
            DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error",
                        "Não foi possível concluir a operação porque há registros relacionados ou um conflito de horário."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(
            Exception exception) {
        log.error("Erro inesperado ao processar a solicitação.", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error",
                        "Ocorreu um erro inesperado. Tente novamente mais tarde."
                ));
    }

    private String fieldNameInPortuguese(String field) {
        return switch (field) {
            case "name" -> "nome";
            case "description" -> "descrição";
            case "address" -> "endereço";
            case "phone" -> "telefone";
            case "email" -> "e-mail";
            case "typeInstitutionId" -> "tipo de instituição";
            case "institutionId" -> "instituição";
            case "locationId" -> "local";
            case "capacity" -> "capacidade";
            case "title" -> "título";
            case "startDate" -> "data e hora de início";
            case "endDate" -> "data e hora de término";
            case "status" -> "status";
            default -> field;
        };
    }
}
