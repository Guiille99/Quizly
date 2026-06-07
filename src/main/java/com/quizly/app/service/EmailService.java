package com.quizly.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.url}")
    private String appUrl;

    public void sendPasswordResetEmail(String userEmail, String token){
        String resetUrl = appUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);

        String htmlContent = templateEngine.process("emails/email_reset-password", context);

        sendHtmlEmail(userEmail, "Recuperación de contraseña - Quizly", htmlContent);
    }

    private void sendHtmlEmail(String userEmail, String subject, String html){
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Error enviando email", e);
        }
    }
}
