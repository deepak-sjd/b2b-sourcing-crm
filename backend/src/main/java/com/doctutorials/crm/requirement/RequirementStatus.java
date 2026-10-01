package com.doctutorials.crm.requirement;

/**
 * Lifecycle of a customer requirement.
 *  OPEN      - captured, nothing done yet
 *  SOURCING  - waiting on supplier replies (set by the Supplier Enquiry module)
 *  QUOTED    - a customer quotation has been prepared for it
 *  CLOSED    - business concluded (Won or Lost is recorded on the lead)
 *  CANCELLED - customer dropped or postponed it; kept for history, never deleted
 */
public enum RequirementStatus { OPEN, SOURCING, QUOTED, CLOSED, CANCELLED }
