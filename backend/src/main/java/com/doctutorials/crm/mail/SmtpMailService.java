package com.doctutorials.crm.mail;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Real email delivery via SMTP (spring-boot-starter-mail + JavaMailSender).
 * Active whenever app.mail.enabled=true (the production default) and a
 * JavaMailSender bean exists, which Spring Boot auto-configures from the
 * spring.mail.* properties.
 *
 * A send failure here must never leak whether an email address exists in
 * the system (see AuthService.forgotPassword), so failures are logged and
 * swallowed - the HTTP caller always gets the same generic response.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your DocTutorials CRM password");
            message.setText("""
                    Hi %s,

                    We received a request to reset your DocTutorials CRM password.

                    Reset it here (this link expires in 30 minutes):
                    %s

                    If you didn't request this, you can safely ignore this email -
                    your password will not be changed.
                    """.formatted(toName, resetLink));
            mailSender.send(message);
        } catch (Exception e) {
            // Never let email delivery failure bubble up to the caller - it would
            // either leak account existence via a 500, or block a legitimate flow
            // over a transient SMTP issue. Log it so it's visible to ops instead.
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }

    @Override
    public void sendSupplierEnquiryEmail(String toEmail, String supplierContactName, String enquiryCode,
            String itemDescription, String quantity, String notes) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Price enquiry " + enquiryCode + " - " + itemDescription);
            message.setText("""
                    Hi %s,

                    We'd like to request a price quote for the following:

                    Item: %s
                    Quantity: %s
                    Enquiry reference: %s
                    %s

                    Please reply to this email with your price, validity, and delivery timeline.

                    Thanks,
                    """.formatted(
                    supplierContactName != null && !supplierContactName.isBlank() ? supplierContactName : "there",
                    itemDescription,
                    quantity != null && !quantity.isBlank() ? quantity : "Not specified",
                    enquiryCode,
                    notes != null && !notes.isBlank() ? "\nAdditional details: " + notes : ""));
            mailSender.send(message);
        } catch (Exception e) {
            // Same reasoning as sendPasswordResetEmail: log and swallow. A supplier
            // with a bad email on file must never block enquiry creation for the
            // other suppliers on the same enquiry - see SupplierEnquiryService.create,
            // which calls this once per supplier and doesn't let one failure abort the rest.
            log.error("Failed to send supplier enquiry email to {}", toEmail, e);
        }
    }
}
