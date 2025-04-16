package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ButtonActionDTO {
    private String buttonId;
    private String buttonText;
    private String action; // "click", "ignore"
    private String reason;

    
}