package com.example.springstarter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class SpringStarterApplication {

    public static void main(String[] args) {
        org.springframework.core.env.Environment env = SpringApplication.run(SpringStarterApplication.class, args).getEnvironment();
        String appName = env.getProperty("spring.application.name", "Spring Application");
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String baseUrl = "http://localhost:" + serverPort + (contextPath.isEmpty() ? "" : contextPath);
        log.info("----------------------------------------------------------");
        log.info("Application '{}' started!", appName);
        log.info("Active profiles: {}", String.join(", ", env.getActiveProfiles()));
        log.info("Base URL: {}", baseUrl);
        log.info("Swagger UI: {}/swagger-ui.html", baseUrl);
        boolean h2Enabled = Boolean.parseBoolean(env.getProperty("spring.h2.console.enabled", "false"));
        if (h2Enabled) {
            String h2Path = env.getProperty("spring.h2.console.path", "/h2-console");
            log.info("H2 Console: {}{}", baseUrl, h2Path);
        }
        String datasourceUrl = env.getProperty("spring.datasource.url");
        if (datasourceUrl != null) {
            log.info("Datasource: {}", datasourceUrl);
        }
        log.info("----------------------------------------------------------");
    }
}
