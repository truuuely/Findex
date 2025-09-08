package com.findex.controller.advice;

import com.findex.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleInvalidJson(
        HttpMessageNotReadableException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Unable to read request body, please check JSON format and field type";
        String causeMsg = Optional.of(e.getMostSpecificCause())
            .map(Throwable::getMessage)
            .orElse(e.getMessage());

        String details = "";
        if (causeMsg != null && !causeMsg.isBlank()) {
            details += "cause: " + causeMsg.strip() + "\n";
        }

        details = details.isBlank() ? null : details.strip();

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(),message, details);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ErrorResponse handleDataIntegrity(
        DataIntegrityViolationException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.CONFLICT;
        List<String> parsed = parseDataIntegrityViolation(e.getMessage());
        String message = parsed.get(0);
        String details = parsed.get(1);

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    private static List<String> parseDataIntegrityViolation(String raw) {
        if (raw == null) return Arrays.asList(null, null);

        String s = raw.strip().replace("\\n", "\n").replace("\\\"", "\"");

        String message = null;
        int err = s.indexOf("ERROR:");
        int det = s.indexOf("Detail:");
        if (err >= 0) {
            int end = s.indexOf('\n', err);
            if (end < 0) end = (det >= 0 ? det : s.length());
            message = s.substring(err + 6, end).strip();
        }

        String details = null;
        if (det >= 0) {
            int end = s.indexOf(']', det);
            if (end < 0) end = s.indexOf('\n', det);
            if (end < 0) end = s.length();
            details = s.substring(det + 7, end).strip();
            int ex = details.indexOf("exists.");
            if (ex >= 0) details = details.substring(0, ex + "exists.".length()).strip();
        }

        return Arrays.asList(message, details);
    }

    private static void log(
        HttpStatus httpStatus,
        HttpServletRequest req,
        String message,
        String details
    ) {
        log.warn("{} {} {} -> {} ({})", httpStatus, req.getMethod(), req.getRequestURI(), message, details);
    }
}
