package com.doctutorials.crm.mail;

import java.time.LocalDate;

/**
 * Everything SmtpMailService needs to compose one supplier-enquiry email.
 * Deliberately excludes the customer/lead name and any budget figure - a
 * supplier should never see who the end customer is or what they're willing
 * to pay (see SupplierEnquiryService, which never passes those in).
 */
public record SupplierEnquiryEmailRequest(
        String toEmail,
        String supplierContactName,
        String enquiryCode,
        String itemDescription,
        String quantity,
        String specs,
        LocalDate deliveryDate,
        LocalDate dueDate,
        String message,
        String senderName,
        String replyToEmail
) {}
