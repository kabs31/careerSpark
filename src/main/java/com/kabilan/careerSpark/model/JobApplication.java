package com.kabilan.careerSpark.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobTitle;
    private String companyName;
    private String jobUrl;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @ManyToOne
    private Resume resume;

    @OneToMany(mappedBy = "jobApplication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormField> formFields;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus submissionStatus;

    @Column(length = 2000)
    private String submissionResult;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (status == null) {
            status = ApplicationStatus.CREATED;
        }

        if (submissionStatus == null) {
            submissionStatus = SubmissionStatus.NOT_SUBMITTED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ApplicationStatus {
        CREATED, ANALYZING_JOB, EXTRACTING_FIELDS, GENERATING_RESPONSES, READY_FOR_REVIEW, SUBMITTING, COMPLETED, FAILED
    }

    public enum SubmissionStatus {
        NOT_SUBMITTED, IN_PROGRESS, SUBMITTED, FAILED
    }
}