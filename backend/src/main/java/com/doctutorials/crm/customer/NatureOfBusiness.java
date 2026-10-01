package com.doctutorials.crm.customer;

/**
 * Stub list — confirm the client's real set of business types and swap these
 * in before go-live. Kept as an enum (not free text like Lead.interest)
 * because this one drives filtering/reporting, so a fixed vocabulary matters
 * more than flexibility here.
 */
public enum NatureOfBusiness {
    MANUFACTURER,
    TRADER,
    SERVICE_PROVIDER,
    DISTRIBUTOR,
    RETAILER,
    OTHER
}
