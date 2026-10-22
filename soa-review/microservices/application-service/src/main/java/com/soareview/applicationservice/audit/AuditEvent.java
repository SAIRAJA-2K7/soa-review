package com.soareview.applicationservice.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "application_service_audit_events")
public class AuditEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 36)
    private String eventId;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 160)
    private String actorId;

    @Column(name = "actor_role", nullable = false, updatable = false, length = 32)
    private String actorRole;

    @Column(name = "operation", nullable = false, updatable = false, length = 16)
    private String operation;

    @Column(name = "resource_type", nullable = false, updatable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", updatable = false, length = 160)
    private String resourceId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "service_name", nullable = false, updatable = false, length = 64)
    private String serviceName;

    @Column(name = "outcome", nullable = false, updatable = false, length = 16)
    private String outcome;

    @Column(name = "http_status", nullable = false, updatable = false)
    private int httpStatus;

    @Column(name = "error_category", updatable = false, length = 64)
    private String errorCategory;

    @Column(name = "correlation_id", updatable = false, length = 160)
    private String correlationId;

    @Column(name = "changed_fields", updatable = false, length = 1000)
    private String changedFields;

    protected AuditEvent() {
    }

    public AuditEvent(String eventId, String actorId, String actorRole, String operation, String resourceType,
                      String resourceId, Instant occurredAt, String serviceName, String outcome, int httpStatus,
                      String errorCategory, String correlationId, String changedFields) {
        this.eventId = eventId;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.operation = operation;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.occurredAt = occurredAt;
        this.serviceName = serviceName;
        this.outcome = outcome;
        this.httpStatus = httpStatus;
        this.errorCategory = errorCategory;
        this.correlationId = correlationId;
        this.changedFields = changedFields;
    }

    public String getEventId() { return eventId; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getOperation() { return operation; }
    public String getResourceType() { return resourceType; }
    public String getResourceId() { return resourceId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getServiceName() { return serviceName; }
    public String getOutcome() { return outcome; }
    public int getHttpStatus() { return httpStatus; }
    public String getErrorCategory() { return errorCategory; }
    public String getCorrelationId() { return correlationId; }
    public String getChangedFields() { return changedFields; }
}
