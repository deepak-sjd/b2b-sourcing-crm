package com.doctutorials.crm.activity;

/** What kind of touchpoint this is. Drives which optional fields (direction,
 *  location) are meaningful and which icon/label the frontend shows. */
public enum ActivityType { CALL, MEETING, SITE_VISIT, EMAIL, WHATSAPP, OTHER }