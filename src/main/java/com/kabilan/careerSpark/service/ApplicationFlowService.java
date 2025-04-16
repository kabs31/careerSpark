package com.kabilan.careerSpark.service;




import com.kabilan.careerSpark.dto.AIResponseDTO;
import com.kabilan.careerSpark.dto.FieldResponseDTO;
import com.kabilan.careerSpark.dto.FormDetailsDTO;
import com.kabilan.careerSpark.model.FormField;
import com.kabilan.careerSpark.model.JobApplication;
import com.kabilan.careerSpark.model.Resume;
import com.kabilan.careerSpark.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationFlowService {

    private final JobApplicationRepository jobApplicationRepository;
    private final FormFieldExtractorService formFieldExtractorService;
    private final AIServiceClient aiServiceClient;
    private final SubmissionService submissionService;

    @Transactional
    public JobApplication createJobApplication(String jobUrl, Resume resume) {
        JobApplication jobApplication = JobApplication.builder()
                .jobUrl(jobUrl)
                .resume(resume)
                .status(JobApplication.ApplicationStatus.CREATED)
                .submissionStatus(JobApplication.SubmissionStatus.NOT_SUBMITTED)
                .build();

        return jobApplicationRepository.save(jobApplication);
    }

    @Async
    @Transactional
    public void processJobApplication(Long jobApplicationId) {
        try {
            JobApplication jobApplication = jobApplicationRepository.findById(jobApplicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Job application not found"));

            // Step 1: Analyze job posting and extract form fields
            updateStatus(jobApplication, JobApplication.ApplicationStatus.ANALYZING_JOB);

            // Step 2: Extract form fields from the application page
            updateStatus(jobApplication, JobApplication.ApplicationStatus.EXTRACTING_FIELDS);
            FormDetailsDTO formDetails = formFieldExtractorService.extractFormFields(
                    jobApplication.getJobUrl(), jobApplication);

            jobApplication.setFormFields(formDetails.getFormFields());
            jobApplicationRepository.save(jobApplication);

            // Step 3: Send to AI service for response generation
            updateStatus(jobApplication, JobApplication.ApplicationStatus.GENERATING_RESPONSES);

            Resume resume = jobApplication.getResume();
            AIResponseDTO aiResponse = aiServiceClient.generateResponses(jobApplication, resume, formDetails);

            // Apply AI-generated responses to form fields
            if (aiResponse.getFieldResponses() != null) {
                for (FieldResponseDTO fieldResponse : aiResponse.getFieldResponses()) {
                    for (FormField field : jobApplication.getFormFields()) {
                        if (field.getId().equals(fieldResponse.getFieldId())) {
                            field.setValue(fieldResponse.getFieldValue());
                            break;
                        }
                    }
                }
                jobApplicationRepository.save(jobApplication);
            }

            // Step 4: Ready for review
            updateStatus(jobApplication, JobApplication.ApplicationStatus.READY_FOR_REVIEW);

        } catch (Exception e) {
            log.error("Error processing job application", e);
            updateStatus(jobApplicationRepository.findById(jobApplicationId).orElse(null),
                    JobApplication.ApplicationStatus.FAILED);
        }
    }

    @Transactional
    public JobApplication updateFormFieldValues(Long applicationId, List<FormField> updatedFields) {
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Job application not found"));

        List<FormField> existingFields = jobApplication.getFormFields();

        for (FormField updatedField : updatedFields) {
            for (FormField existingField : existingFields) {
                if (existingField.getId().equals(updatedField.getId())) {
                    existingField.setValue(updatedField.getValue());
                    break;
                }
            }
        }

        return jobApplicationRepository.save(jobApplication);
    }

    @Async
    @Transactional
    public void submitApplication(Long applicationId) {
        try {
            JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Job application not found"));

            updateStatus(jobApplication, JobApplication.ApplicationStatus.SUBMITTING);
            jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.IN_PROGRESS);
            jobApplicationRepository.save(jobApplication);

            // Attempt submission
            submissionService.submitJobApplication(jobApplication);

            // Final status updates are handled by the SubmissionService

        } catch (Exception e) {
            log.error("Error submitting application", e);

            Optional<JobApplication> jobApplicationOpt = jobApplicationRepository.findById(applicationId);
            if (jobApplicationOpt.isPresent()) {
                JobApplication jobApplication = jobApplicationOpt.get();
                jobApplication.setStatus(JobApplication.ApplicationStatus.FAILED);
                jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.FAILED);
                jobApplication.setSubmissionResult("Error: " + e.getMessage());
                jobApplicationRepository.save(jobApplication);
            }
        }
    }

    private void updateStatus(JobApplication application, JobApplication.ApplicationStatus status) {
        if (application != null) {
            application.setStatus(status);
            jobApplicationRepository.save(application);
        }
    }
}