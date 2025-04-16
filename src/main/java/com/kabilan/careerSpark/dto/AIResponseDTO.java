package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIResponseDTO {
    private List<FieldResponseDTO> fieldResponses;
    private ButtonActionDTO buttonAction;
    private boolean isSubmissionComplete;
    private String message;

    public AIResponseDTO(Object fieldResponses, Object buttonAction, boolean isSubmissionComplete, String message) {
        if (fieldResponses instanceof List<?>) {
            this.fieldResponses = (List<FieldResponseDTO>) fieldResponses;
        } else {
            this.fieldResponses = null; // or handle the case appropriately
        }
        this.buttonAction = (ButtonActionDTO) buttonAction;
        this.isSubmissionComplete = isSubmissionComplete;
        this.message = message;
    }

    
}