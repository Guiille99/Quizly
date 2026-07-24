package com.quizly.app.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "quiz")
@Getter
@Setter
public class QuizProperties {
    private Duration inactivityMaxDuration = Duration.ofHours(1);
    private Duration activityUpdateInterval = Duration.ofMinutes(1);
}
