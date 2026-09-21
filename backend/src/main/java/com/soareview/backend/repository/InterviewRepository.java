package com.soareview.backend.repository;

import com.soareview.backend.entity.InterviewEntity;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<InterviewEntity, Long> {
    List<InterviewEntity> findByCandidateOrderByScheduledAtAsc(UserEntity candidate);
    List<InterviewEntity> findByJobOrderByScheduledAtAsc(JobEntity job);
}
