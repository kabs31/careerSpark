package com.kabilan.careerSpark.service;


import com.kabilan.careerSpark.dto.ButtonDTO;
import com.kabilan.careerSpark.dto.FormDetailsDTO;
import com.kabilan.careerSpark.model.FormField;
import com.kabilan.careerSpark.model.JobApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormFieldExtractorService {

    private final WebDriver webDriver;
    private final AIServiceClient aiServiceClient;

    public FormDetailsDTO extractFormFields(String jobUrl, JobApplication jobApplication) {
        try {
            log.info("Navigating to job application URL: {}", jobUrl);
            webDriver.get(jobUrl);

            // Allow page to load fully
            Thread.sleep(5000);

            // Find form elements
            List<WebElement> inputElements = webDriver.findElements(By.tagName("input"));
            List<WebElement> textareaElements = webDriver.findElements(By.tagName("textarea"));
            List<WebElement> selectElements = webDriver.findElements(By.tagName("select"));

            List<FormField> formFields = new ArrayList<>();

            // Process input elements
            for (WebElement input : inputElements) {
                String type = input.getAttribute("type");
                // Skip hidden, submit, button inputs
                if (type == null || type.equals("hidden") || type.equals("submit") ||
                        type.equals("button") || type.equals("reset")) {
                    continue;
                }

                String id = input.getAttribute("id");
                String name = input.getAttribute("name");
                String fieldId = id != null && !id.isEmpty() ? id : name;

                if (fieldId == null || fieldId.isEmpty()) {
                    continue; // Skip elements without id or name
                }

                // Try to find label
                String label = findLabelForElement(input);

                // Determine if required
                boolean required = input.getAttribute("required") != null ||
                        input.getAttribute("aria-required") != null;

                // Check for select options if it's a dropdown disguised as input
                List<String> options = new ArrayList<>();
                try {
                    input.click();
                    Thread.sleep(500);
                    List<WebElement> dropdownOptions = webDriver.findElements(By.cssSelector("li, option, div[role='option']"));
                    if (!dropdownOptions.isEmpty()) {
                        for (WebElement option : dropdownOptions) {
                            String optionText = option.getText();
                            if (optionText != null && !optionText.trim().isEmpty()) {
                                options.add(optionText.trim());
                            }
                        }
                        // Click somewhere else to close dropdown
                        webDriver.findElement(By.tagName("body")).click();
                    }
                } catch (Exception e) {
                    log.debug("Not a clickable input or no dropdown options: {}", fieldId);
                }

                FormField formField = FormField.builder()
                        .fieldId(fieldId)
                        .fieldName(name)
                        .fieldLabel(label)
                        .fieldType(mapInputTypeToFieldType(type))
                        .required(required)
                        .jobApplication(jobApplication)
                        .build();

                formFields.add(formField);
            }

            // Process textarea elements
            for (WebElement textarea : textareaElements) {
                String id = textarea.getAttribute("id");
                String name = textarea.getAttribute("name");
                String fieldId = id != null && !id.isEmpty() ? id : name;

                if (fieldId == null || fieldId.isEmpty()) {
                    continue;
                }

                String label = findLabelForElement(textarea);
                boolean required = textarea.getAttribute("required") != null ||
                        textarea.getAttribute("aria-required") != null;

                FormField formField = FormField.builder()
                        .fieldId(fieldId)
                        .fieldName(name)
                        .fieldLabel(label)
                        .fieldType(FormField.FieldType.TEXTAREA)
                        .required(required)
                        .jobApplication(jobApplication)
                        .build();



                formFields.add(formField);
            }

            // Process select elements
            for (WebElement select : selectElements) {
                String id = select.getAttribute("id");
                String name = select.getAttribute("name");
                String fieldId = id != null && !id.isEmpty() ? id : name;

                if (fieldId == null || fieldId.isEmpty()) {
                    continue;
                }

                String label = findLabelForElement(select);
                boolean required = select.getAttribute("required") != null ||
                        select.getAttribute("aria-required") != null;

                // Get options
                List<String> options = new ArrayList<>();
                try {
                    List<WebElement> optionElements = select.findElements(By.tagName("option"));
                    for (WebElement option : optionElements) {
                        String optionText = option.getText();
                        if (optionText != null && !optionText.trim().isEmpty()) {
                            options.add(optionText.trim());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not extract options from select: {}", fieldId, e);
                }

                FormField formField = FormField.builder()
                        .fieldId(fieldId)
                        .fieldName(name)
                        .fieldLabel(label)
                        .fieldType(FormField.FieldType.SELECT)
                        .required(required)
                        .jobApplication(jobApplication)
                        .build();

                formFields.add(formField);
            }

            // Extract job title and company name
            String pageTitle = "";
            String pageContent = "";
            try {
                pageTitle = webDriver.getTitle();
                pageContent = webDriver.findElement(By.tagName("body")).getText();

                String jobTitle = webDriver.findElement(By.cssSelector("h1")).getText();
                jobApplication.setJobTitle(jobTitle);

                // Company name is harder to reliably extract without specific selectors
                // This is just a simple example approach
                List<WebElement> possibleCompanyElements = webDriver.findElements(
                        By.cssSelector("h2, .company-name, [itemprop='hiringOrganization']"));

                if (!possibleCompanyElements.isEmpty()) {
                    jobApplication.setCompanyName(possibleCompanyElements.get(0).getText());
                }
            } catch (Exception e) {
                log.warn("Could not extract job title or company name", e);
            }

            // Find buttons
            List<ButtonDTO> buttons = findButtons();

            FormDetailsDTO formDetails = new FormDetailsDTO();
            formDetails.setFormFields(formFields);
            formDetails.setButtons(buttons);
            formDetails.setPageTitle(pageTitle);
            formDetails.setPageContent(pageContent);

            return formDetails;

        } catch (Exception e) {
            log.error("Error extracting form fields", e);
            throw new RuntimeException("Failed to extract application form fields", e);
        } finally {
            try {
                webDriver.quit();
            } catch (Exception e) {
                log.error("Error closing WebDriver", e);
            }
        }
    }

    private List<ButtonDTO> findButtons() {
        List<ButtonDTO> buttons = new ArrayList<>();
        try {
            List<WebElement> buttonElements = webDriver.findElements(By.tagName("button"));
            buttonElements.addAll(webDriver.findElements(By.cssSelector("input[type='submit'], input[type='button']")));
            buttonElements.addAll(webDriver.findElements(By.cssSelector("a.button, .btn, [role='button']")));

            for (WebElement button : buttonElements) {
                String id = button.getAttribute("id");
                String text = button.getText();
                String name = button.getAttribute("name");
                String cssClass = button.getAttribute("class");

                if ((text == null || text.isEmpty()) && button.findElements(By.tagName("span")).size() > 0) {
                    text = button.findElement(By.tagName("span")).getText();
                }

                if (id != null || name != null || (text != null && !text.isEmpty())) {
                    ButtonDTO buttonDTO = new ButtonDTO();
                    buttonDTO.setId(id);
                    buttonDTO.setName(name);
                    buttonDTO.setText(text);
                    buttonDTO.setCssClass(cssClass);
                    buttons.add(buttonDTO);
                }
            }
        } catch (Exception e) {
            log.warn("Error finding buttons", e);
        }
        return buttons;
    }

    private String findLabelForElement(WebElement element) {
        String id = element.getAttribute("id");
        if (id != null && !id.isEmpty()) {
            try {
                WebElement label = webDriver.findElement(By.cssSelector("label[for='" + id + "']"));
                return label.getText();
            } catch (Exception e) {
                // Label not found by for attribute
            }
        }

        // Try aria-label
        String ariaLabel = element.getAttribute("aria-label");
        if (ariaLabel != null && !ariaLabel.isEmpty()) {
            return ariaLabel;
        }

        // Try placeholder
        String placeholder = element.getAttribute("placeholder");
        if (placeholder != null && !placeholder.isEmpty()) {
            return placeholder;
        }

        // Try name attribute
        String name = element.getAttribute("name");
        if (name != null && !name.isEmpty()) {
            return name.replace("_", " ").replace("-", " ");
        }

        // Try looking for a label near the element
        try {
            // Get element position
            Point location = element.getLocation();
            Dimension size = element.getSize();

            // Find labels in the vicinity (above or to the left)
            List<WebElement> nearbyLabels = webDriver.findElements(By.xpath(
                    "//label[not(@for) and (preceding::* or following::*)]"));

            for (WebElement label : nearbyLabels) {
                Point labelLocation = label.getLocation();
                if ((Math.abs(labelLocation.getX() - location.getX()) < 300) &&
                        labelLocation.getY() < location.getY() &&
                        location.getY() - labelLocation.getY() < 100) {
                    return label.getText();
                }
            }
        } catch (Exception e) {
            // Failed to find label by proximity
        }

        return "Unlabeled Field";
    }

    private FormField.FieldType mapInputTypeToFieldType(String inputType) {
        switch (inputType) {
            case "text": return FormField.FieldType.TEXT;
            case "email": return FormField.FieldType.EMAIL;
            case "password": return FormField.FieldType.PASSWORD;
            case "number": return FormField.FieldType.NUMBER;
            case "checkbox": return FormField.FieldType.CHECKBOX;
            case "radio": return FormField.FieldType.RADIO;
            case "date": return FormField.FieldType.DATE;
            case "file": return FormField.FieldType.FILE;
            default: return FormField.FieldType.TEXT;
        }
    }


}

