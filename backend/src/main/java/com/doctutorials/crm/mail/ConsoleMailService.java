package com.doctutorials.crm.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Local-dev stand-in for real email: logs instead of sending, so you can
 * test full flows (forgot-password, supplier enquiries) without an SMTP
 * account. Active when app.mail.enabled=false.
 *
 * Set CRM_MAIL_ENABLED=false in your local .env while you don't have SMTP
 * creds yet; flip it to true (the default) once you add real ones in
 * application.yml / env vars, and this bean stops being selected.
 */
@Service
@ConditionalOnProperty(prefix = "app.mail", name = "enabled", havingValue = "false")
public class ConsoleMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailService.class);

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        log.warn("[DEV MAIL - app.mail.enabled=false, no email actually sent] " +
                "Password reset link for {} <{}>: {}", toName, toEmail, resetLink);
    }

    @Override
    public boolean sendSupplierEnquiryEmail(SupplierEnquiryEmailRequest request) {
        log.warn("[DEV MAIL - app.mail.enabled=false, no email actually sent] " +
                "Supplier enquiry {} to {} <{}>: {} (qty: {})",
                request.enquiryCode(), request.supplierContactName(), request.toEmail(),
                request.itemDescription(), request.quantity());
        return true;
    }
}
