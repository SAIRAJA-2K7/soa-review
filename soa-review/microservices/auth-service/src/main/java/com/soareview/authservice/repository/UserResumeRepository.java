package com.soareview.authservice.repository;

import com.soareview.authservice.entity.UserResume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserResumeRepository extends JpaRepository<UserResume, Long> {
}
