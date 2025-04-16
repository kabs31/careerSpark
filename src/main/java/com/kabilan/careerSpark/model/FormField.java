package com.kabilan.careerSpark.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fieldId;
    private String fieldName;
    private String fieldLabel;

    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    private boolean required;
    private boolean aiGenerated;

    @Column(length = 5000)
    private String value;

    @ElementCollection
    @CollectionTable(name = "form_field_options", joinColumns = @JoinColumn(name = "form_field_id"))
    @Column(name = "option")
    private List<String> options;

    @ManyToOne
    private JobApplication jobApplication;

    

    public enum FieldType {
        TEXT, EMAIL, PASSWORD, NUMBER, TEXTAREA, SELECT, CHECKBOX, RADIO, DATE, FILE
    }

    
}
