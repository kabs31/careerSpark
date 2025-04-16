package com.kabilan.careerSpark.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIRequestDTO {
    private String jobTitle;
    private String companyName;
    private String pageTitle;
    private String pageContent;
    private ResumeDTO resume;
    private List<FormFieldDTO> formFields;
    private List<ButtonDTO> buttons;

   
}