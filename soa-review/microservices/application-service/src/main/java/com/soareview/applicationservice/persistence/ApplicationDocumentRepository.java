package com.soareview.applicationservice.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    List<ApplicationDocument> findByCollectionName(String collectionName);
    Optional<ApplicationDocument> findByCollectionNameAndRecordKey(String collectionName, String recordKey);
}