package com.doctutorials.crm.document;

// Which single FK on Document is set - drives /documents/by/{parentType}/{parentId}
// and which service's getById() does the visibility check on upload.
public enum DocumentParentType { LEAD, CUSTOMER, SUPPLIER, REQUIREMENT, QUOTATION, ENQUIRY_REPLY }