package com.soareview.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserEntity applicant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "application_timeline", joinColumns = @JoinColumn(name = "application_id"))
    private List<ApplicationTimelineItem> timeline = new ArrayList<>();

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationTimelineItem {
        @Column(nullable = false)
        private String stage;

        @Column
        private String date;

        @Column(nullable = false)
        private boolean completed;

        @Column
        private String note;
    }
}
