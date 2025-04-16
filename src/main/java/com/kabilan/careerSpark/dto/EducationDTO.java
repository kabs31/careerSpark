package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationDTO {
    private String institution;
    private String degree;
    private String field;
    private String startDate;
    private String endDate;
    private Double gpa;
}