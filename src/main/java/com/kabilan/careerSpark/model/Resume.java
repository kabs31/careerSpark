package com.kabilan.careerSpark.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;

    private String fullName;
    private String email;
    private String phone;

    @Column(length = 5000)
    private String summary;

    @ElementCollection
    @CollectionTable(name = "resume_skills", joinColumns = @JoinColumn(name = "resume_id"))
    @Column(name = "skill")
    private List<String> skills;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "resume_id")
    private List<WorkExperience> workExperience;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "resume_id")
    private List<Education> education;

    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        uploadedAt = LocalDateTime.now();
    }

    public void setFullName(String s) {
    }

    public void setEmail(String s) {
    }

    public void setPhone(String s) {
    }

    public void setSkills(List<String> skills) {
    }

    public void setWorkExperience(List<WorkExperience> experiences) {
    }

    public void setEducation(List<Education> educations) {
    }

    public void setSummary(String summary) {
    }

    
}