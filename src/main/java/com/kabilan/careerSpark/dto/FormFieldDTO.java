package com.kabilan.careerSpark.dto;

import com.kabilan.careerSpark.model.FormField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldDTO {
    private Long id;
    private String fieldId;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private List<String> options;
    private boolean required;

    

    public static FormFieldDTO fromEntity(FormField formField) {
        if (formField == null) return null;

        return new FormFieldDTO(
                (Long) formField.getId(),
                (String) formField.getFieldId(),
                (String) formField.getFieldName(),
                (String) formField.getFieldLabel(),
                String.valueOf(formField.getFieldType()),
                formField.getOptions(),
                formField.isRequired()
        );
    }
}