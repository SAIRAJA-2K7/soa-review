package com.soareview.backend.repository;

import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.SavedJobEntity;
import com.soareview.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedJobRepository extends JpaRepository<SavedJobEntity, Long> {
    List<SavedJobEntity> findByUser(UserEntity user);
    Optional<SavedJobEntity> findByUserAndJob(UserEntity user, JobEntity job);
}
