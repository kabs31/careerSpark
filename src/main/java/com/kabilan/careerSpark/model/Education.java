package com.kabilan.careerSpark.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String institution;
    private String degree;
    private String fieldOfStudy;

    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyEnrolled;

    private String location;
    private Double gpa;

    @Column(length = 1000)
    private String description;

    // This method is needed to match the call in AIRequestDTO
    public String getField() {
        return fieldOfStudy;
    }
}