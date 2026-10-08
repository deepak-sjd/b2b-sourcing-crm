package com.doctutorials.crm.mail;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Real email delivery via SMTP (spring-boot-starter-mail + JavaMailSender).
 * Active whenever app.mail.enabled=true (the production default).
 *
 * A reset-email failure must never leak whether an address exists in the
 * system (see sendPasswordResetEmail - swallowed, always logged). A
 * supplier-enquiry failure is different: the caller (SupplierEnquiryService)
 * needs to know, so it can show "delivery failed" instead of silently
 * looking like nothing happened - hence the boolean return there.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

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
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }

    @Override
    public boolean sendSupplierEnquiryEmail(SupplierEnquiryEmailRequest req) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            if (req.replyToEmail() != null && !req.replyToEmail().isBlank()) {
                message.setReplyTo(req.replyToEmail());
            }
            message.setTo(req.toEmail());
            // Strip CR/LF so a crafted item description can't inject extra headers
            // via the subject line.
            String safeSubject = ("Price enquiry " + req.enquiryCode() + " - " + req.itemDescription())
                    .replaceAll("[\\r\\n]+", " ");
            message.setSubject(safeSubject);
            message.setText(buildBody(req));
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send supplier enquiry email to {}", req.toEmail(), e);
            return false;
        }
    }

    // Deliberately no customer/lead name, no budget figure - a supplier should
    // never see who the end customer is or what they're willing to pay.
    private String buildBody(SupplierEnquiryEmailRequest req) {
        StringBuilder sb = new StringBuilder();
        String greeting = req.supplierContactName() != null && !req.supplierContactName().isBlank()
                ? req.supplierContactName() : "there";
        sb.append("Hi ").append(greeting).append(",\n\n");
        sb.append("We'd like to request a price quote for the following:\n\n");
        sb.append("Item: ").append(req.itemDescription()).append("\n");
        sb.append("Quantity: ").append(req.quantity() != null && !req.quantity().isBlank() ? req.quantity() : "Not specified").append("\n");
        if (req.specs() != null && !req.specs().isBlank()) {
            sb.append("Specifications: ").append(req.specs()).append("\n");
        }
        if (req.deliveryDate() != null) {
            sb.append("Needed by: ").append(req.deliveryDate().format(DATE_FORMAT)).append("\n");
        }
        if (req.message() != null && !req.message().isBlank()) {
            sb.append("Additional details: ").append(req.message()).append("\n");
        }
        sb.append("Enquiry reference: ").append(req.enquiryCode()).append("\n");
        if (req.dueDate() != null) {
            sb.append("\nPlease reply by ").append(req.dueDate().format(DATE_FORMAT)).append(" with your price, validity, and delivery timeline.\n");
        } else {
            sb.append("\nPlease reply with your price, validity, and delivery timeline.\n");
        }
        sb.append("\nThanks,\n").append(req.senderName() != null ? req.senderName() : "");
        return sb.toString();
    }
}
