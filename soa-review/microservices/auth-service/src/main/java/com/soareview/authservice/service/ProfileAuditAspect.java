package com.soareview.authservice.service;

import com.soareview.authservice.entity.AuthAuditEvent;
import com.soareview.authservice.repository.AuthAuditEventRepository;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
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
public class ProfileAuditAspect {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "password", "token", "secret", "authorization", "otp", "email");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "(?i)[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}");

    private final AuthAuditEventRepository repository;
    private final PlatformTransactionManager transactionManager;

    public ProfileAuditAspect(AuthAuditEventRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionManager = transactionManager;
    }

    @Around("execution(public * com.soareview.authservice.controller.UserProfileController.*(..))")
    public Object auditProfileOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) return joinPoint.proceed();

        TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            Object result = joinPoint.proceed();
            repository.saveAndFlush(event(request, result, "SUCCESS", 200, null, joinPoint.getArgs()));
            transactionManager.commit(transaction);
            return result;
        } catch (Throwable failure) {
            if (!transaction.isCompleted()) transactionManager.rollback(transaction);
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
            if (!transaction.isCompleted()) transactionManager.rollback(transaction);
            if (auditFailure != failure) failure.addSuppressed(auditFailure);
        }
    }

    private AuthAuditEvent event(HttpServletRequest request, Object result, String outcome, int status,
                                 String errorCategory, Object[] arguments) {
        String actorId = valueOr(request.getHeader("X-User-Id"), request.getHeader("X-User-Email"), "anonymous");
        String resourceId = result instanceof Map<?, ?> map && map.get("id") != null
                ? String.valueOf(map.get("id")) : targetId(arguments, actorId);
        String operation = "GET".equals(request.getMethod()) ? "READ" : "UPDATE";
        return new AuthAuditEvent(
                UUID.randomUUID().toString(),
                actorId,
                valueOr(request.getHeader("X-User-Role"), "UNKNOWN"),
                operation,
                "USER_PROFILE",
                resourceId,
                Instant.now(),
                outcome,
                status,
                errorCategory,
                correlationId(request),
                changedFields(request.getMethod(), arguments));
    }

    private String targetId(Object[] arguments, String fallback) {
        return Arrays.stream(arguments)
                .filter(Long.class::isInstance)
                .map(String::valueOf)
                .findFirst()
                .orElse(fallback);
    }

    private String changedFields(String method, Object[] arguments) {
        if (!"PATCH".equals(method)) return null;
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
