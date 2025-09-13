package com.findex.controller.advice;

import com.findex.dto.response.ErrorResponse;
import com.findex.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidJson(
        HttpMessageNotReadableException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Unable baseDateTo read request body, please check JSON format and field type";
        String causeMsg = Optional.of(e.getMostSpecificCause())
            .map(Throwable::getMessage)
            .orElse(e.getMessage());

        String details = "";
        if (causeMsg != null && !causeMsg.isBlank()) {
            details += "cause: " + causeMsg.strip() + "\n";
        }

        details = details.isBlank() ? null : details.strip();

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(
        ConstraintViolationException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Request parameter value not valid";
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String details = violations == null || violations.isEmpty() ? null :
            "violations:\n" + violations.stream()
                .map(cv -> "- " + propertyPath(cv)
                    + " | message=" + safe(cv.getMessage())
                    + " | invalid=" + safe(cv.getInvalidValue()))
                .collect(Collectors.joining("\n"));

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(
        MethodArgumentNotValidException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "Request body value not valid";

        String details = toValidationDetails(e.getBindingResult());

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleParameterTypeValidation(
        MethodArgumentTypeMismatchException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "parameter=%s, value=%s, expectedType=%s".formatted(
            e.getName(), e.getValue(), typeName(e.getRequiredType())
        );

        String details = joinDetails("parameterTypeMismatch:",
            "- parameter=" + e.getName(),
            "- value=" + safe(e.getValue()),
            "- expectedType=" + typeName(e.getRequiredType()),
            e.getCause() != null ? "- cause=" + safe(e.getCause().getMessage()) : null
        );

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(
        MissingServletRequestParameterException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "missing parameter: %s (required type: %s)".formatted(
            e.getParameterName(), e.getParameterType()
        );

        String details = joinDetails("missingParameter:",
            "- name=" + e.getParameterName(),
            "- requiredType=" + safe(e.getParameterType()),
            "- message=" + safe(e.getMessage())
        );

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingPart(
        MissingServletRequestPartException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = "missing part: " + e.getRequestPartName();

        String details = joinDetails("missingPart:",
            "- part=" + e.getRequestPartName(),
            e.getMessage() != null ? "- message=" + safe(e.getMessage()) : null
        );

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler({ NoHandlerFoundException.class, NoResourceFoundException.class })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoHandler(
        Exception e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = (e.getMessage() != null) ? e.getMessage() : "Endpoint not found";

        log(status, req, message, null);

        return new ErrorResponse(Instant.now(), status.value(), message, null);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
        NotFoundException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        String message = (e.getMessage() != null) ? e.getMessage() : "Resource not found";

        log(status, req, message, null);

        return new ErrorResponse(Instant.now(), status.value(), message, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorResponse handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.METHOD_NOT_ALLOWED;
        String message = "Method not allowed: " + e.getMethod();

        String allowed = (e.getSupportedHttpMethods() == null || e.getSupportedHttpMethods().isEmpty())
            ? null
            : e.getSupportedHttpMethods().stream().map(HttpMethod::name)
            .collect(Collectors.joining(", "));

        String details = joinDetails("methodNotAllowed:",
            "- method=" + req.getMethod(),
            "- path=" + req.getRequestURI(),
            (allowed != null && !allowed.isBlank()) ? "- allowed=" + allowed : null
        );

        log(status, req, message, details);

        return new ErrorResponse(Instant.now(), status.value(), message, details);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
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

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ErrorResponse handleMediaTypeNotSupported(
        HttpMediaTypeNotSupportedException e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        String supported = !e.getSupportedMediaTypes().isEmpty()
            ? " Supported types: " + e.getSupportedMediaTypes().stream()
            .map(Object::toString)
            .collect(Collectors.joining(", "))
            : "";
        String message = "Media type not allowed." + supported;

        log(status, req, message, null);

        return new ErrorResponse(Instant.now(), status.value(), message, null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAny(
        Exception e,
        HttpServletRequest req
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Unexpected error occurred";

        log(status, req, message, e.toString());

        return new ErrorResponse(Instant.now(), status.value(), message, null);
    }

    private static String toValidationDetails(BindingResult br) {
        if (br == null) return null;

        List<FieldError> fieldErrors = br.getFieldErrors();
        List<ObjectError> globalErrors = br.getGlobalErrors();

        if (fieldErrors.isEmpty() && globalErrors.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(256);
        if (!fieldErrors.isEmpty()) {
            sb.append("fieldErrors:");
            for (FieldError fe : fieldErrors) {
                sb.append('\n')
                    .append("- ")
                    .append(fe.getField())
                    .append(" | message=")
                    .append(safe(fe.getDefaultMessage()))
                    .append(" | rejected=")
                    .append(safe(fe.getRejectedValue()));
            }
        }
        if (!globalErrors.isEmpty()) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append("globalErrors:");
            for (ObjectError ge : globalErrors) {
                sb.append('\n')
                    .append("- ")
                    .append(ge.getObjectName())
                    .append(" | message=")
                    .append(safe(ge.getDefaultMessage()));
            }
        }
        return sb.toString();
    }

    private static String joinDetails(String title, String... lines) {
        StringJoiner sj = new StringJoiner("\n");
        if (title != null && !title.isBlank()) sj.add(title);
        for (String line : lines) {
            if (line != null && !line.isBlank()) sj.add(line);
        }
        String out = sj.toString();
        return out.isBlank() ? null : out;
    }

    private static String typeName(Class<?> c) {
        return c == null ? "unknown" : c.getSimpleName();
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

    private static String propertyPath(ConstraintViolation<?> cv) {
        return cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : "";
    }

    private static String safe(Object v) {
        if (v == null) return "null";
        return String.valueOf(v).strip().replace("\n", "\\n");
    }

    private static void log(
        HttpStatus status,
        HttpServletRequest req,
        String message,
        String details
    ) {
        log.warn("{} {} {} -> {} \n {}", status, req.getMethod(), req.getRequestURI(), message, details);
    }
}
