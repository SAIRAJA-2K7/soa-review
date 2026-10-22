package com.soareview.jobservice.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobDocumentStore {

    private static final TypeReference<Map<String, Object>> DOCUMENT_TYPE = new TypeReference<>() { };

    private final JobDocumentRepository repository;
    private final ObjectMapper objectMapper;

    public JobDocumentStore(JobDocumentRepository repository, ObjectMapper objectMapper) {
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
    public List<Map<String, Object>> findAllByKeys(String collection, List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        return repository.findByCollectionNameAndRecordKeyIn(collection, keys).stream()
                .map(document -> decode(document.getPayload()))
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
        JobDocument document = repository.findByCollectionNameAndRecordKey(collection, key)
                .orElseGet(() -> new JobDocument(collection, key, "{}"));
        document.setPayload(encode(value));
        repository.save(document);
        return value;
    }

    @Transactional
    public Map<String, Object> create(String collection, Map<String, Object> value) {
        JobDocument document = repository.saveAndFlush(
                new JobDocument(collection, UUID.randomUUID().toString(), encode(value)));
        value.put("id", document.getId());
        document.setRecordKey(String.valueOf(document.getId()));
        document.setPayload(encode(value));
        repository.save(document);
        return value;
    }

    @Transactional
    public void delete(String collection, String key) {
        repository.deleteByCollectionNameAndRecordKey(collection, key);
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
            throw new IllegalStateException("Could not decode persisted job data", exception);
        }
    }

    private String encode(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not encode job data", exception);
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