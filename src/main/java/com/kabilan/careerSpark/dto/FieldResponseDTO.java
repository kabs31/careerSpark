package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldResponseDTO {
    private Long fieldId;
    private String fieldValue;

}