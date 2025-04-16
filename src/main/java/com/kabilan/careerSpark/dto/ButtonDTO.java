package com.kabilan.careerSpark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ButtonDTO {
    private String id;
    private String name;
    private String text;
    private String cssClass;

    public void setId(String id) {
    }

    public void setName(String name) {
    }

    public void setText(String text) {
    }

    public void setCssClass(String cssClass) {
    }
}
