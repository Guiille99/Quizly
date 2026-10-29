package com.quizly.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.quizly.app.exception.QuizlyException;
import com.quizly.app.logging.LogExecution;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @LogExecution
    public void sendPasswordResetEmail(String userEmail, String token, String appUrl) throws QuizlyException{
        String resetUrl = appUrl + "/reset-password?token=" + token;

        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);

        String htmlContent = templateEngine.process("emails/email_reset-password", context);

        sendHtmlEmail(userEmail, "Recuperación de contraseña - Quizly", htmlContent);
    }

    @LogExecution
    public void sendGoogleAccountEmail(String userEmail, String appUrl) throws QuizlyException{
        Context context = new Context();
        context.setVariable("loginUrl", appUrl + "/login");

        String htmlContent = templateEngine.process("emails/email_google-account", context);

        sendHtmlEmail(userEmail, "Accede a Quizly con Google", htmlContent);
    }

    private void sendHtmlEmail(String userEmail, String subject, String html) throws QuizlyException{
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Quizly");
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

            log.info("Correo '{}' enviado correctamente", subject);
        } catch (Exception e) {
            log.error("Se ha producido un error al enviar el email", e);
            throw new QuizlyException("Se ha producido un error al enviar el email.");
        }
    }
}
