package com.kabilan.careerSpark.controller;

import com.kabilan.careerSpark.model.FormField;
import com.kabilan.careerSpark.model.JobApplication;
import com.kabilan.careerSpark.model.Resume;
import com.kabilan.careerSpark.repository.JobApplicationRepository;
import com.kabilan.careerSpark.repository.ResumeRepository;
import com.kabilan.careerSpark.service.ApplicationFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationRepository jobApplicationRepository;
    private final ResumeRepository resumeRepository;
    private final ApplicationFlowService applicationFlowService;

    @GetMapping
    public List<JobApplication> getAllApplications() {
        return jobApplicationRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobApplication> getApplicationById(@PathVariable Long id) {
        return jobApplicationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/resume/{resumeId}")
    public List<JobApplication> getApplicationsByResumeId(@PathVariable Long resumeId) {
        return jobApplicationRepository.findByResumeId(resumeId);
    }

    @GetMapping("/recent-completed")
    public List<JobApplication> getRecentCompletedApplications() {
        return jobApplicationRepository.findRecentCompletedApplications();
    }

    @PostMapping("/create")
    public ResponseEntity<JobApplication> createApplication(@RequestBody Map<String, String> request) {
        String jobUrl = request.get("jobUrl");
        Long resumeId = Long.parseLong(request.get("resumeId"));

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));

        JobApplication jobApplication = applicationFlowService.createJobApplication(jobUrl, resume);

        // Start asynchronous processing
        applicationFlowService.processJobApplication(jobApplication.getId());

        return ResponseEntity.ok(jobApplication);
    }

    @PutMapping("/{id}/fields")
    public ResponseEntity<JobApplication> updateFormFields(
            @PathVariable Long id,
            @RequestBody List<FormField> updatedFields) {
        try {
            JobApplication updated = applicationFlowService.updateFormFieldValues(id, updatedFields);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<String> submitApplication(@PathVariable Long id) {
        if (!jobApplicationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        applicationFlowService.submitApplication(id);
        return ResponseEntity.accepted().body("Application submission initiated");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        if (jobApplicationRepository.existsById(id)) {
            jobApplicationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getApplicationStatus(@PathVariable Long id) {
        return jobApplicationRepository.findById(id)
                .map(app -> {
                    Map<String, Object> status = Map.of(
                            "status", app.getStatus(),
                            "submissionStatus", app.getSubmissionStatus(),
                            "submissionResult", app.getSubmissionResult() != null ?  app.getSubmissionResult() : "",
                            "updatedAt", app.getUpdatedAt()
                    );
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}