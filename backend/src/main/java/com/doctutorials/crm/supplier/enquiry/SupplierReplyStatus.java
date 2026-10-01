package com.doctutorials.crm.supplier.enquiry;

/** PENDING from the moment the enquiry email goes out; moves to REPLIED once
 *  someone logs the supplier's price, or DECLINED if the supplier can't quote. */
public enum SupplierReplyStatus {
    PENDING, REPLIED, DECLINED
}
