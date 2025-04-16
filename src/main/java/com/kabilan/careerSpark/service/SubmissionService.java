package com.kabilan.careerSpark.service;


import com.kabilan.careerSpark.dto.AIResponseDTO;
import com.kabilan.careerSpark.dto.ButtonActionDTO;
import com.kabilan.careerSpark.model.FormField;
import com.kabilan.careerSpark.model.JobApplication;
import com.kabilan.careerSpark.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final JobApplicationRepository jobApplicationRepository;
    private final AIServiceClient aiServiceClient;

    @Value("${app.selenium.headless}")
    private boolean headless;

    @Value("${app.selenium.timeout}")
    private int timeout;

    

    public void submitJobApplication(JobApplication jobApplication) {
        WebDriver webDriver = null;

        try {
            // Initialize WebDriver
            ChromeOptions options = new ChromeOptions();
            if (headless) {
                options.addArguments("--headless=new");
            }
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--remote-allow-origins=*");

            webDriver = new ChromeDriver(options);
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(timeout));

            // Navigate to job URL
            webDriver.get(jobApplication.getJobUrl());
            log.info("Navigated to URL: {}", jobApplication.getJobUrl());

            // Allow page to load
            Thread.sleep(3000);

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(timeout));

            // Fill in each form field
            fillFormFields(webDriver, jobApplication);

            // Submit the application
            submitNextStep(webDriver, jobApplication, aiServiceClient.analyzePage(jobApplication, webDriver.findElement(By.tagName("body")).getText()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread was interrupted", e);
            jobApplication.setStatus(JobApplication.ApplicationStatus.FAILED);
            jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.FAILED);
            jobApplication.setSubmissionResult("Thread was interrupted: " + e.getMessage());
            jobApplicationRepository.save(jobApplication);
        } catch (Exception e) {
            log.error("Unexpected error during job application submission", e);
            jobApplication.setStatus(JobApplication.ApplicationStatus.FAILED);
            jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.FAILED);
            jobApplication.setSubmissionResult("Unexpected error: " + e.getMessage());
            jobApplicationRepository.save(jobApplication);
        } finally {
            if (webDriver != null) {
                try {
                    webDriver.quit();
                } catch (Exception e) {
                    log.error("Error closing WebDriver", e);
                }
            }
        }
    }

    private void submitNextStep(WebDriver webDriver, JobApplication jobApplication, AIResponseDTO aiResponse) {
        try {
            ButtonActionDTO buttonAction = aiResponse.getButtonAction();
            WebElement button = null;

            // Try by ID first
            if (buttonAction.getButtonId() != null && !buttonAction.getButtonId().isEmpty()) {
                try {
                    button = webDriver.findElement(By.id(buttonAction.getButtonId()));
                } catch (Exception e) {
                    log.info("Button not found by ID: {}", buttonAction.getButtonId());
                }
            }

            // If not found by ID, try by text
            if (button == null && buttonAction.getButtonText() != null && !buttonAction.getButtonText().isEmpty()) {
                try {
                    List<WebElement> possibleButtons = webDriver.findElements(
                            By.xpath("//button[contains(text(),'" + buttonAction.getButtonText() + "')] | " +
                                    "//input[@value='" + buttonAction.getButtonText() + "'] | " +
                                    "//a[contains(text(),'" + buttonAction.getButtonText() + "')]")
                    );

                    if (!possibleButtons.isEmpty()) {
                        button = possibleButtons.get(0);
                    }
                } catch (Exception e) {
                    log.info("Button not found by text: {}", buttonAction.getButtonText());
                }
            }

            if (button != null) {
                button.click();
                log.info("Clicked next button: {}", buttonAction.getButtonText());

                Thread.sleep(5000);

                // Check if we're done
                String finalPageContent = webDriver.findElement(By.tagName("body")).getText();
                AIResponseDTO finalCheck = aiServiceClient.analyzePage(jobApplication, finalPageContent);

                if (finalCheck.isSubmissionComplete()) {
                    updateApplicationToComplete(jobApplication, finalCheck.getMessage());
                } else {
                    jobApplication.setStatus(JobApplication.ApplicationStatus.COMPLETED);
                    jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.SUBMITTED);
                    jobApplication.setSubmissionResult("Application steps completed, but final confirmation not detected.");
                    jobApplication.setSubmittedAt(LocalDateTime.now());
                    jobApplicationRepository.save(jobApplication);
                }
            } else {
                jobApplication.setStatus(JobApplication.ApplicationStatus.FAILED);
                jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.FAILED);
                jobApplication.setSubmissionResult("Could not find button for next submission step");
                jobApplicationRepository.save(jobApplication);
            }
        } catch (Exception e) {
            log.error("Error in next submission step", e);
            jobApplication.setStatus(JobApplication.ApplicationStatus.FAILED);
            jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.FAILED);
            jobApplication.setSubmissionResult("Error in next submission step: " + e.getMessage());
            jobApplicationRepository.save(jobApplication);
        }
    }

    private void updateApplicationToComplete(JobApplication jobApplication, String message) {
        jobApplication.setStatus(JobApplication.ApplicationStatus.COMPLETED);
        jobApplication.setSubmissionStatus(JobApplication.SubmissionStatus.SUBMITTED);
        jobApplication.setSubmissionResult("Application successfully submitted: " + message);
        jobApplication.setSubmittedAt(LocalDateTime.now());
        jobApplicationRepository.save(jobApplication);
        log.info("Application marked as complete: {}", jobApplication.getId());
    }

    private String getFieldSelector(FormField field) {
        if (field.getFieldId() != null && !field.getFieldId().isEmpty()) {
            return "#" + field.getFieldId();
        } else if (field.getFieldName() != null && !field.getFieldName().isEmpty()) {
            return "[name='" + field.getFieldName() + "']";
        } else {
            throw new IllegalArgumentException("Field has no id or name: " + field);
        }
    }

    private void fillFormFields(WebDriver webDriver, JobApplication jobApplication) {
        for (FormField field : jobApplication.getFormFields()) {
            if (field.getValue() == null || field.getValue().isEmpty()) {
                continue; // Skip empty fields
            }

            try {
                String fieldSelector = getFieldSelector(field);
                WebElement element = webDriver.findElement(By.cssSelector(fieldSelector));

                fillFormField(webDriver, element, field);
                log.info("Filled field: {}", field.getFieldId());

                // Short pause between fields to avoid detection
                Thread.sleep(300);
            } catch (Exception e) {
                log.warn("Could not fill field: {}", field.getFieldId(), e);
                // Continue with other fields
            }
        }
    }

    private void fillFormField(WebDriver webDriver, WebElement element, FormField field) {
        try {
            switch (field.getFieldType()) {
                case TEXT:
                case EMAIL:
                case PASSWORD:
                case NUMBER:
                    element.clear();
                    element.sendKeys(field.getValue());
                    break;

                case TEXTAREA:
                    element.clear();
                    element.sendKeys(field.getValue());
                    break;

                case SELECT:
                    Select select = new Select(element);
                    select.selectByVisibleText(field.getValue());
                    break;

                case CHECKBOX:
                    boolean check = Boolean.parseBoolean(field.getValue());
                    if (check && !element.isSelected()) {
                        element.click();
                    } else if (!check && element.isSelected()) {
                        element.click();
                    }
                    break;

                case RADIO:
                    if (!element.isSelected()) {
                        element.click();
                    }
                    break;

                case DATE:
                    element.clear();
                    element.sendKeys(field.getValue());
                    break;

                case FILE:
                    element.sendKeys(field.getValue()); // Value should be file path
                    break;

                default:
                    log.warn("Unsupported field type: {}", field.getFieldType());
            }
        } catch (ElementNotInteractableException e) {
            log.warn("Element not interactable: {}", field.getFieldId(), e);
            // Try JavaScript as fallback
            try {
                ((JavascriptExecutor) webDriver).executeScript(
                        "arguments[0].value = arguments[1];",
                        element,
                        field.getValue()
                );
            } catch (Exception jsException) {
                log.error("Failed to interact with element via JavaScript", jsException);
                throw e;
            }
        }
    }
}