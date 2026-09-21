package com.soareview.backend.repository;

import com.soareview.backend.entity.ApplicationEntity;
import com.soareview.backend.entity.JobEntity;
import com.soareview.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, Long> {
    List<ApplicationEntity> findByApplicantOrderByAppliedAtDesc(UserEntity applicant);
    List<ApplicationEntity> findByJobOrderByAppliedAtDesc(JobEntity job);
    List<ApplicationEntity> findByApplicantAndJob(UserEntity applicant, JobEntity job);
}
