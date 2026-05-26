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

            String normalizedRecipient = to.toLowerCase();
            if (!(normalizedRecipient.contains("example") || normalizedRecipient.contains("test"))) {
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

    public String buildAuthActionEmailHtml(
            @NotBlank String appName,
            @NotBlank String emailTitle,
            @NotBlank String heading,
            @NotBlank String bodyCopy,
            @NotBlank String actionLabel,
            @NotBlank String actionUrl,
            int expiryMinutes,
            @NotBlank String footerCopy) {
        String safeAppName = escapeHtml(appName);
        String safeEmailTitle = escapeHtml(emailTitle);
        String safeHeading = escapeHtml(heading);
        String safeBodyCopy = escapeHtml(bodyCopy);
        String safeActionLabel = escapeHtml(actionLabel);
        String safeActionUrl = escapeHtml(actionUrl);
        String safeFooterCopy = escapeHtml(footerCopy);

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>%s - %s</title>
                </head>
                <body style="margin:0; padding:0; background:#101322; color:#f5f7ff; font-family:Verdana, Geneva, sans-serif;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:linear-gradient(135deg,#101322 0%%,#1b2040 100%%); padding:28px 12px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;">
                                    <tr>
                                        <td align="center" style="padding:0 0 16px 0;">
                                            <h1 style="margin:0; font-size:32px; letter-spacing:1px; color:#ffffff;">MEDIA ODYSSEY</h1>
                                            <p style="margin:8px 0 0 0; color:#d6defd; font-size:15px;">Discover your next obsession!</p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background:#171b33; border:1px solid #2a325f; border-radius:16px; padding:28px 24px;">
                                            <h2 style="margin:0 0 12px 0; color:#ffffff; font-size:24px;">%s</h2>
                                            <p style="margin:0 0 18px 0; color:#d6defd; font-size:15px; line-height:1.6;">
                                                %s
                                            </p>
                                            <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 8px auto;">
                                                <tr>
                                                    <td align="center" bgcolor="#20c9ff" style="border-radius:10px;">
                                                        <a href="%s" style="display:inline-block; padding:12px 22px; font-weight:700; color:#0b1023; text-decoration:none; font-size:15px;">
                                                            %s
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>
                                            <p style="margin:18px 0 0 0; color:#c2caef; font-size:13px; line-height:1.6;">
                                                This link expires in %d minutes.
                                            </p>
                                            <p style="margin:16px 0 0 0; color:#c2caef; font-size:13px; line-height:1.6;">
                                                If the button does not work, copy and paste this link into your browser:
                                            </p>
                                            <p style="margin:8px 0 0 0; word-break:break-all; font-size:13px;">
                                                <a href="%s" style="color:#7ee1ff; text-decoration:underline;">%s</a>
                                            </p>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center" style="padding:16px 8px 0 8px; color:#9fa8d1; font-size:12px; line-height:1.5;">
                                            %s
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(
                        safeAppName,
                        safeEmailTitle,
                        safeHeading,
                        safeBodyCopy,
                        safeActionUrl,
                        safeActionLabel,
                        expiryMinutes,
                        safeActionUrl,
                        safeActionUrl,
                        safeFooterCopy);
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