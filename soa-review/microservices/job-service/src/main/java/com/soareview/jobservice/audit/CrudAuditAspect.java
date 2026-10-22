package com.soareview.jobservice.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CrudAuditAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "token", "secret", "authorization", "otp", "email");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}");

    private final AuditEventRepository repository;
    private final PlatformTransactionManager transactionManager;

    public CrudAuditAspect(AuditEventRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionManager = transactionManager;
    }

    @Around("execution(public * com.soareview.jobservice.controller..*(..))")
    public Object auditControllerOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || !isAuditedPath(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Object result = joinPoint.proceed();
            repository.saveAndFlush(event(request, result, "SUCCESS", 200, null, joinPoint.getArgs()));
            transactionManager.commit(transaction);
            return result;
        } catch (Throwable failure) {
            if (!transaction.isCompleted()) {
                transactionManager.rollback(transaction);
            }
            recordFailure(request, joinPoint.getArgs(), failure);
            throw failure;
        }
    }

    private void recordFailure(HttpServletRequest request, Object[] arguments, Throwable failure) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus transaction = transactionManager.getTransaction(definition);
        try {
            int status = failure instanceof ResponseStatusException responseStatus
                    ? responseStatus.getStatusCode().value() : HttpStatus.INTERNAL_SERVER_ERROR.value();
            repository.saveAndFlush(event(request, null, "FAILURE", status,
                    failure instanceof ResponseStatusException ? "REQUEST_REJECTED" : "INTERNAL_ERROR", arguments));
            transactionManager.commit(transaction);
        } catch (RuntimeException auditFailure) {
            if (!transaction.isCompleted()) {
                transactionManager.rollback(transaction);
            }
            if (auditFailure != failure) {
                failure.addSuppressed(auditFailure);
            }
        }
    }

    private AuditEvent event(HttpServletRequest request, Object result, String outcome, int status,
                             String errorCategory, Object[] arguments) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String resource = resourceType(path, request.getHeader("X-User-Role"));
        String operation = operation(method);
        return new AuditEvent(
                UUID.randomUUID().toString(),
                valueOr(request.getHeader("X-User-Id"), request.getHeader("X-User-Email"), "anonymous"),
                valueOr(request.getHeader("X-User-Role"), "ANONYMOUS"),
                operation,
                resource,
                resourceId(path, result),
                Instant.now(),
                "job-service",
                outcome,
                status,
                errorCategory,
                correlationId(request),
                changedFields(method, arguments));
    }

    private boolean isAuditedPath(String path) {
        return path.matches("/api/jobs(?:/\\d+)?")
                || path.matches("/api/user/saved-jobs(?:/\\d+)?")
                || path.matches("/api/admin/jobs");
    }

    private String resourceType(String path, String role) {
        if (path.startsWith("/api/user/saved-jobs")) return "SAVED_JOB";
        if (path.startsWith("/api/admin/jobs")) return "JOB_LIST";
        return "JOB";
    }

    private String operation(String method) {
        return switch (method) {
            case "GET" -> "READ";
            case "POST" -> "CREATE";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private String resourceId(String path, Object result) {
        if (result instanceof Map<?, ?> map && map.get("id") != null) {
            return String.valueOf(map.get("id"));
        }
        var matcher = java.util.regex.Pattern.compile("/(\\d+)(?:/.*)?$").matcher(path);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private String changedFields(String method, Object[] arguments) {
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) return null;
        return Arrays.stream(arguments)
                .filter(Map.class::isInstance)
                .map(argument -> (Map<?, ?>) argument)
                .flatMap(map -> map.keySet().stream().map(String::valueOf))
                .filter(name -> SENSITIVE_FIELDS.stream().noneMatch(name.toLowerCase()::contains))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return null;
        return attributes.getRequest();
    }

    private String valueOr(String... values) {
        return Arrays.stream(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse("");
    }

    private String correlationId(HttpServletRequest request) {
        String candidate = valueOr(request.getHeader("X-Correlation-ID"), request.getHeader("X-Request-ID"));
        return UUID_PATTERN.matcher(candidate).matches()
                ? candidate.toLowerCase(Locale.ROOT)
                : UUID.randomUUID().toString();
    }
}
