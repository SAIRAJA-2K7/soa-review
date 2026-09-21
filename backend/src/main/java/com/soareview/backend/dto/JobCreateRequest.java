package com.soareview.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class JobCreateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String companyName;

    @NotBlank
    private String location;

    @NotBlank
    private String type = "FULL_TIME";

    @NotBlank
    private String experience;

    private Integer salaryMin;
    private Integer salaryMax;

    @NotBlank
    private String description;

    private List<String> responsibilities = List.of();
    private List<String> requirements = List.of();
    private List<String> skills = List.of();
}
