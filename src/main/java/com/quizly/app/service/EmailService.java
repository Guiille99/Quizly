package com.quizly.app.service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.quizly.app.exception.QuizlyException;
import com.quizly.app.logging.LogExecution;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final RestClient restClient;
    private final String fromEmail;
    private final String gmailRefreshToken;
    private final String googleClientId;
    private final String googleClientSecret;

    @Autowired
    public EmailService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            @Value("${spring.mail.username}") String fromEmail,
            @Value("${email.gmail.refresh-token:}") String gmailRefreshToken,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret}") String googleClientSecret) {
        this(mailSender, templateEngine, fromEmail, gmailRefreshToken, googleClientId, googleClientSecret,
                createRestClient());
    }

    EmailService(
            JavaMailSender mailSender,
            SpringTemplateEngine templateEngine,
            String fromEmail,
            String gmailRefreshToken,
            String googleClientId,
            String googleClientSecret,
            RestClient restClient) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.gmailRefreshToken = gmailRefreshToken;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
        this.restClient = restClient;
    }

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

            if (gmailRefreshToken == null || gmailRefreshToken.isBlank()) {
                mailSender.send(message);
            } else {
                sendViaGmailApi(message);
            }

            log.info("Correo '{}' enviado correctamente", subject);
        } catch (Exception e) {
            log.error("Se ha producido un error al enviar el email", e);
            throw new QuizlyException("Se ha producido un error al enviar el email.");
        }
    }

        /**
         * Envío de correo electrónico utilizando la API de Gmail.
         * @param message
         * @throws Exception
         */
        private void sendViaGmailApi(MimeMessage message) throws Exception {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        form.add("refresh_token", gmailRefreshToken);
        form.add("grant_type", "refresh_token");

        Map<?, ?> tokenResponse = restClient.post()
                .uri(GOOGLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        message.writeTo(rawMessage);

        restClient.post()
                .uri(GMAIL_SEND_URL)
                .headers(headers -> headers.setBearerAuth((String) tokenResponse.get("access_token")))
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("raw", Base64.getUrlEncoder().encodeToString(rawMessage.toByteArray())))
                .retrieve()
                .toBodilessEntity();
    }

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
