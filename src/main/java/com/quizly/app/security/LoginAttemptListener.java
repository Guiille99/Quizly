package com.quizly.app.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Cuenta los logins fallidos por usuario (para {@link LoginThrottleFilter}) y los reinicia al acertar. */
@Component
public class LoginAttemptListener {

    private final RateLimiter rateLimiter;

    public LoginAttemptListener(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @EventListener
    public void onBadCredentials(AuthenticationFailureBadCredentialsEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null) {
            rateLimiter.hit(LoginThrottleFilter.key(username), LoginThrottleFilter.WINDOW);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        if (username != null) {
            rateLimiter.reset(LoginThrottleFilter.key(username));
        }
    }
}
