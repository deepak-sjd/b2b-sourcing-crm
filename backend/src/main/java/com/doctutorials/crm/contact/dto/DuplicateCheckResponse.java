package com.doctutorials.crm.contact.dto;

/** Result of the "is this a duplicate contact" AI-assist check run while
 *  typing on the create form. match is null when nothing was found. */
public record DuplicateCheckResponse(
        boolean duplicate,
        ContactResponse match,
        String matchedOn
) {
    public static DuplicateCheckResponse none() {
        return new DuplicateCheckResponse(false, null, null);
    }

    public static DuplicateCheckResponse of(ContactResponse match, String matchedOn) {
        return new DuplicateCheckResponse(true, match, matchedOn);
    }
}
