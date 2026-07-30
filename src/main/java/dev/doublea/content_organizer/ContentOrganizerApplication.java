package dev.doublea.content_organizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import dev.doublea.content_organizer.config.JwtProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class ContentOrganizerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentOrganizerApplication.class, args);
    }
}
