package dev.doublea.content_organizer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(value = "app.jwt")
public record JwtProperties(String secretKey, long expiration) {} 