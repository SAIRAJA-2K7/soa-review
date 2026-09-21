package com.soareview.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private Long id;
    private String title;
    private CompanyDto company;
    private String location;
    private String type;
    private String experience;
    private Integer salaryMin;
    private Integer salaryMax;
    private String posted;
    private List<String> skills;
    private String description;
    private List<String> responsibilities;
    private List<String> requirements;
    private boolean saved;
    private boolean applied;
    private String recruiter;
}
