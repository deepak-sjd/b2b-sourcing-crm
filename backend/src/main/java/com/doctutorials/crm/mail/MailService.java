package com.doctutorials.crm.mail;

/**
 * Abstraction over "send this person an email" so AuthService doesn't care
 * whether it's real SMTP or a local dev stand-in. Swap implementations via
 * app.mail.enabled without touching any calling code.
 */
public interface MailService {
    void sendPasswordResetEmail(String toEmail, String toName, String resetLink);

    /**
     * PRD 12.2: "Send one enquiry by email to all suppliers, or to selected
     * suppliers." One call per supplier (not one email with everyone CC'd) so
     * a supplier never sees who else was asked. Returns whether delivery
     * actually succeeded - callers (SupplierEnquiryService) must record a
     * false result as a failure, not silently treat the attempt as a send.
     */
    boolean sendSupplierEnquiryEmail(SupplierEnquiryEmailRequest request);
}
