package com.kabilan.careerSpark.config;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConfigurationProperties(prefix = "app.resume")
@Getter
@Setter
public class FileStorageConfig {

    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            if (!StringUtils.hasText(uploadDir)) {
                uploadDir = "./uploads/resumes";
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
