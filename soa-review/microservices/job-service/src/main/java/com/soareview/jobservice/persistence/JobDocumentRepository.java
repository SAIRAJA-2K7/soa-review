package com.soareview.jobservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JobDocumentRepository extends JpaRepository<JobDocument, Long> {
    List<JobDocument> findByCollectionName(String collectionName);
    List<JobDocument> findByCollectionNameAndRecordKeyIn(String collectionName, Collection<String> recordKeys);
    Optional<JobDocument> findByCollectionNameAndRecordKey(String collectionName, String recordKey);
    void deleteByCollectionNameAndRecordKey(String collectionName, String recordKey);
}