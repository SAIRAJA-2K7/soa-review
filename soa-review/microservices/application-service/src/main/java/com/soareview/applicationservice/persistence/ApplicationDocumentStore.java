package com.soareview.applicationservice.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationDocumentStore {

    private static final TypeReference<Map<String, Object>> DOCUMENT_TYPE = new TypeReference<>() { };

    private final ApplicationDocumentRepository repository;
    private final ObjectMapper objectMapper;

    public ApplicationDocumentStore(ApplicationDocumentRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findAll(String collection) {
        return repository.findByCollectionName(collection).stream()
                .map(document -> decode(document.getPayload()))
                .sorted(Comparator.comparingLong(document -> numberValue(document.get("id"))))
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> find(String collection, String key) {
        return repository.findByCollectionNameAndRecordKey(collection, key)
                .map(document -> decode(document.getPayload()))
                .orElse(null);
    }

    @Transactional
    public Map<String, Object> save(String collection, String key, Map<String, Object> value) {
        ApplicationDocument document = repository.findByCollectionNameAndRecordKey(collection, key)
                .orElseGet(() -> new ApplicationDocument(collection, key, "{}"));
        document.setPayload(encode(value));
        repository.save(document);
        return value;
    }

    @Transactional
    public Map<String, Object> create(String collection, Map<String, Object> value) {
        ApplicationDocument document = repository.saveAndFlush(
                new ApplicationDocument(collection, UUID.randomUUID().toString(), encode(value)));
        value.put("id", document.getId());
        document.setRecordKey(String.valueOf(document.getId()));
        document.setPayload(encode(value));
        repository.save(document);
        return value;
    }

    @Transactional
    public Map<String, Object> createUnique(String collection, String reservationCollection,
                                            String reservationKey, Map<String, Object> value) {
        ApplicationDocument reservation = repository.saveAndFlush(
                new ApplicationDocument(reservationCollection, reservationKey, "{}"));
        ApplicationDocument document = repository.saveAndFlush(
                new ApplicationDocument(collection, UUID.randomUUID().toString(), encode(value)));
        value.put("id", document.getId());
        document.setRecordKey(String.valueOf(document.getId()));
        document.setPayload(encode(value));
        repository.save(document);
        reservation.setPayload(String.valueOf(document.getId()));
        repository.save(reservation);
        return value;
    }

    @Transactional(readOnly = true)
    public long nextId(String collection) {
        return findAll(collection).stream()
                .mapToLong(document -> numberValue(document.get("id")))
                .max().orElse(0L) + 1L;
    }

    private Map<String, Object> decode(String payload) {
        try {
            return objectMapper.readValue(payload, DOCUMENT_TYPE);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not decode persisted application data", exception);
        }
    }

    private String encode(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode application data", exception);
        }
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}