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
public class ApplicationDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String status;
    private String appliedDate;
    private String nextInterview;
    private List<ApplicationTimelineItemDto> timeline;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationTimelineItemDto {
        private String stage;
        private String date;
        private boolean completed;
        private String note;
    }
}
