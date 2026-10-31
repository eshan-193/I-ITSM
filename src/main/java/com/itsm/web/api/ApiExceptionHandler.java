package com.itsm.web.api;

import com.itsm.exception.ItsmException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/** Returns RFC 9457 "problem detail" JSON for errors from the REST API. */
@RestControllerAdvice(basePackages = "com.itsm.web.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(ItsmException.class)
    public ResponseEntity<ProblemDetail> handle(ItsmException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(ex.httpStatus()), ex.getMessage());
        problem.setTitle(ex.getClass().getSimpleName().replace("Exception", ""));
        return ResponseEntity.status(ex.httpStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation");
        return ResponseEntity.badRequest().body(problem);
    }
}
