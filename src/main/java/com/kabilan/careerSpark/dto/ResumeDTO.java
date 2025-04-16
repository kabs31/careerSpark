package com.kabilan.careerSpark.dto;

import com.kabilan.careerSpark.model.Resume;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDTO {
    private String fullName;
    private String email;
    private String phone;
    private String summary;
    private List<String> skills;
    private List<WorkExperienceDTO> workExperience;
    private List<EducationDTO> education;

    public static ResumeDTO fromEntity(Resume resume) {
        if (resume == null) return null;

        ResumeDTO dto = new ResumeDTO();
        dto.setFullName(resume.getFullName());
        dto.setEmail(resume.getEmail());
        dto.setPhone(resume.getPhone());
        dto.setSummary(resume.getSummary());
        dto.setSkills(resume.getSkills());

        if (resume.getWorkExperience() != null) {
            dto.setWorkExperience(resume.getWorkExperience().stream()
                    .map(we -> new WorkExperienceDTO(
                            we.getCompany(),
                            we.getPosition(),
                            we.getStartDate() != null ? we.getStartDate().toString() : null,
                            we.getEndDate() != null ? we.getEndDate().toString() : null,
                            we.isCurrentPosition(),
                            we.getDescription()
                    ))
                    .toList());
        }

        if (resume.getEducation() != null) {
            dto.setEducation(resume.getEducation().stream()
                    .map(edu -> new EducationDTO(
                            edu.getInstitution(),
                            edu.getDegree(),
                            edu.getFieldOfStudy(),
                            edu.getStartDate() != null ? edu.getStartDate().toString() : null,
                            edu.getEndDate() != null ? edu.getEndDate().toString() : null,
                            edu.getGpa()
                    ))
                    .toList());
        }

        return dto;
    }
}