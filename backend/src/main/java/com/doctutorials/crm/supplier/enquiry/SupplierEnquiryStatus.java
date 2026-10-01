package com.doctutorials.crm.supplier.enquiry;

/** OPEN while waiting on replies; CLOSED once a reply has been chosen as the
 *  price reference (SupplierEnquiryService.chooseReply flips this). */
public enum SupplierEnquiryStatus {
    OPEN, CLOSED
}
