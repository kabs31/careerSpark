package com.kabilan.careerSpark.service;




import com.kabilan.careerSpark.dto.AIRequestDTO;
import com.kabilan.careerSpark.dto.AIResponseDTO;

import com.kabilan.careerSpark.dto.FormDetailsDTO;
import com.kabilan.careerSpark.dto.FormFieldDTO;
import com.kabilan.careerSpark.dto.ResumeDTO;

import com.kabilan.careerSpark.model.JobApplication;
import com.kabilan.careerSpark.model.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceClient {

    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Value("${app.ai-service.endpoints.analyze-form}")
    private String analyzeFormEndpoint;

    @Value("${app.ai-service.endpoints.generate-responses}")
    private String generateResponsesEndpoint;

    @Value("${app.ai-service.endpoints.analyze-page}")
    private String analyzePageEndpoint;

   

    public AIResponseDTO generateResponses(JobApplication jobApplication, Resume resume, FormDetailsDTO formDetails) {
        try {
            String url = aiServiceUrl + generateResponsesEndpoint;

            AIRequestDTO requestDTO = createRequestDTO(jobApplication, resume, formDetails);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<AIRequestDTO> request = new HttpEntity<>(requestDTO, headers);

            ResponseEntity<AIResponseDTO> response = restTemplate.postForEntity(
                    url, request, AIResponseDTO.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Received null response from AI service");
            }

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error calling AI service", e);
            throw new RuntimeException("Failed to generate AI responses", e);
        }
    }

    public AIResponseDTO analyzePage(JobApplication jobApplication, String pageContent) {
        try {
            String url = aiServiceUrl + analyzePageEndpoint;

            Map<String, Object> requestMap = Map.of(
                    "jobApplicationId", jobApplication.getId(),
                    "pageContent", pageContent
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestMap, headers);

            ResponseEntity<AIResponseDTO> response = restTemplate.postForEntity(
                    url, request, AIResponseDTO.class);

            if (response.getBody() == null) {
                return new AIResponseDTO(null, null, false, "Failed to analyze page");
            }

            return response.getBody();

        } catch (RestClientException e) {
            log.error("Error calling AI service for page analysis", e);
            return new AIResponseDTO(null, null, false, "Error: " + e.getMessage());
        }
    }

    private AIRequestDTO createRequestDTO(JobApplication jobApplication, Resume resume, FormDetailsDTO formDetails) {
        List<FormFieldDTO> formFieldDTOs = formDetails.getFormFields().stream()
                .map(FormFieldDTO::fromEntity)
                .collect(Collectors.toList());

        return AIRequestDTO.builder()
                .jobTitle(jobApplication.getJobTitle())
                .companyName(jobApplication.getCompanyName())
                .pageTitle(formDetails.getPageTitle())
                .pageContent(formDetails.getPageContent())
                .resume(ResumeDTO.fromEntity(resume))
                .formFields(formFieldDTOs)
                .buttons(formDetails.getButtons())
                .build();
    }

    public String generateText(String prompt) {
        // Implement your logic here to generate text based on the prompt
        return "Generated text based on the prompt"; // Placeholder return value
    }
}