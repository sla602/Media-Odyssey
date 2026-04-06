package com.mo.mediaodyssey.shared.services;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

import jakarta.validation.constraints.NotBlank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException;

@Service
@Validated
public class EmailService {
    // Inspiried by
    // https://github.com/resend/resend-examples/blob/main/java-resend-examples/spring_boot_app/src/main/java/com/resend/springboot/EmailController.java
    // Debugging assisted by AI.

    private final Resend resend;
    private final String from;

    public EmailService(
            @Value("${resend.api.key:}") String apiKey,
            @Value("${email.from:}") String from) {
        this.resend = new Resend(apiKey);
        this.from = from;
    }

    public void sendEmail(@NotBlank String to, @NotBlank String subject, @NotBlank String message) {
        sendHtmlEmail(to, subject, "<p>" + escapeHtml(message) + "</p>");
    }

    public void sendHtmlEmail(@NotBlank String to, @NotBlank String subject, @NotBlank String htmlMessage) {
        try {
            // Silently prevent sending to common test email addresses, such as email
            // addresses containing "example" or "test".
            // This is necessary to prevent suspension to email sending by Resend.

            if (!(to.contains("example") || to.contains("test"))) {
                var params = CreateEmailOptions.builder()
                        .from(from)
                        .to(to)
                        .subject(subject)
                        .html(htmlMessage)
                        .build();

                resend.emails().send(params);
            }
        } catch (Exception e) {
            throw new DisabledException("Unable to send email");
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}