package com.quizly.app.service;

public interface AiTokenCounter {
    int countTokens(String model, String systemInstruction, String userPrompt);
}