package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperienceDTO {
    private String company;
    private String position;
    private String startDate;
    private String endDate;
    private boolean currentPosition;
    private String description;
}