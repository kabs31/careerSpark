package com.kabilan.careerSpark.dto;

import com.kabilan.careerSpark.model.FormField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormDetailsDTO {
    private List<FormField> formFields;
    private List<ButtonDTO> buttons;
    private String pageTitle;
    private String pageContent;


}
