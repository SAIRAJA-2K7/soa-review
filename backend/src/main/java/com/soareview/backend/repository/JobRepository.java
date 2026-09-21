package com.soareview.backend.repository;

import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<JobEntity, Long> {
    List<JobEntity> findByPublishedTrueOrderByPostedAtDesc();
    List<JobEntity> findByRecruiterOrderByPostedAtDesc(UserEntity recruiter);
}
