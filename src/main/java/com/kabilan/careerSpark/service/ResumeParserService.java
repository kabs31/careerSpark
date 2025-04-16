package com.kabilan.careerSpark.service;

import com.kabilan.careerSpark.config.FileStorageConfig;
import com.kabilan.careerSpark.model.Education;
import com.kabilan.careerSpark.model.Resume;
import com.kabilan.careerSpark.model.WorkExperience;
import com.kabilan.careerSpark.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeParserService {

    private final FileStorageConfig fileStorageConfig;
    private final ResumeRepository resumeRepository;
    private final AIServiceClient aiServiceClient;
    private final Tika tika = new Tika();

    public Resume parseAndStoreResume(MultipartFile file) throws IOException {
        // Store the file
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID().toString() + fileExtension;

        Path targetLocation = Paths.get(fileStorageConfig.getUploadDir()).resolve(newFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Parse the resume
        String resumeText = extractTextFromResume(targetLocation.toFile());

        // Use AI to extract structured information
        return parseResumeWithAI(resumeText, originalFilename, newFilename, targetLocation.toString(), file.getContentType(), file.getSize());
    }

    private String extractTextFromResume(java.io.File file) throws IOException {
        try {
            BodyContentHandler handler = new BodyContentHandler(-1);
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            java.io.FileInputStream stream = new java.io.FileInputStream(file);
            parser.parse(stream, handler, metadata, context);

            return handler.toString();
        } catch (TikaException | SAXException e) {
            log.error("Error parsing resume file", e);
            throw new IOException("Could not parse resume content", e);
        }
    }

    private Resume parseResumeWithAI(String resumeText, String originalFilename, String newFilename, String filePath, String contentType, long fileSize) {
        // For a basic implementation, we'll extract some patterns directly
        // In a production app, we would use more sophisticated NER or pass to an LLM

        String prompt = "Extract the following information from this resume: " +
                "full name, email, phone number, a brief professional summary, " +
                "skills (as a comma-separated list), work experience (with company, position, dates, description), " +
                "and education (with institution, degree, dates). Format as JSON.\n\nResume text: " + resumeText;

        String aiResponse = aiServiceClient.generateText(prompt);

        // For now, we'll use regex as a fallback and combine with partial parsing
        // In a real implementation, we would properly parse the AI JSON response

        Resume resume = Resume.builder()
                .fileName(originalFilename)
                .filePath(filePath)
                .contentType(contentType)
                .fileSize(fileSize)
                .build();

        // Extract basic information with regex as a fallback
        resume.setFullName(extractFullName(resumeText));
        resume.setEmail(extractEmail(resumeText));
        resume.setPhone(extractPhone(resumeText));

        // Extract skills (simplified approach)
        String skillsSection = extractSection(resumeText, "Skills", "Experience");
        List<String> skills = new ArrayList<>();
        if (skillsSection != null) {
            skills = Arrays.asList(skillsSection.split(",|•|\\|"));
            skills.replaceAll(String::trim);
        }
        resume.setSkills(skills);

        // Create dummy work experience for demo
        List<WorkExperience> experiences = new ArrayList<>();
        experiences.add(WorkExperience.builder()
                .company("Example Company")
                .position("Software Engineer")
                .startDate(LocalDate.of(2020, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .currentPosition(false)
                .description("Developed web applications using Spring Boot and React")
                .build());

        resume.setWorkExperience(experiences);

        // Create dummy education for demo
        List<Education> educations = new ArrayList<>();
        educations.add(Education.builder()
                .institution("Example University")
                .degree("Bachelor of Science")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2016, 9, 1))
                .endDate(LocalDate.of(2020, 5, 31))
                .build());

        resume.setEducation(educations);

        // Extract or generate summary
        String summary = extractSection(resumeText, "Summary", "Experience");
        if (summary == null || summary.trim().isEmpty()) {
            // Generate a summary with AI if not found
            String summaryPrompt = "Generate a brief professional summary based on this resume: " + resumeText;
            summary = aiServiceClient.generateText(summaryPrompt);
        }
        resume.setSummary(summary);

        return resumeRepository.save(resume);
    }

    private String extractFullName(String text) {
        // This is a simplified approach - in reality would need more robust extraction
        Pattern pattern = Pattern.compile("^([A-Z][a-z]+ [A-Z][a-z]+)");
        Matcher matcher = pattern.matcher(text.trim());
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "John Doe"; // Default fallback
    }

    private String extractEmail(String text) {
        Pattern pattern = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "example@email.com"; // Default fallback
    }

    private String extractPhone(String text) {
        Pattern pattern = Pattern.compile("\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return "(123) 456-7890"; // Default fallback
    }

    private String extractSection(String text, String sectionName, String nextSectionName) {
        // Very basic section extraction - would need improvement for production
        int sectionStart = text.indexOf(sectionName);
        if (sectionStart == -1) return null;

        int sectionEnd = text.indexOf(nextSectionName, sectionStart);
        if (sectionEnd == -1) sectionEnd = text.length();

        return text.substring(sectionStart + sectionName.length(), sectionEnd).trim();
    }
}
