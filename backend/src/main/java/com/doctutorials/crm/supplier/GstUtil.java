package com.doctutorials.crm.supplier;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Deterministic GSTIN parsing/validation - this is what the "auto-fill state
 * from GST number" and "GST looks invalid" affordances on the supplier form
 * are built on. Not a call to any external service (there's no live GSTN
 * lookup wired up), just the public GSTIN structure + its checksum digit, so
 * it works offline and instantly as the user types.
 *
 * Format: 2 digits (state code) + 10 chars (PAN) + 1 digit (entity number)
 * + 1 char (Z by convention) + 1 checksum char = 15 chars total.
 */
public final class GstUtil {

    private GstUtil() {
    }

    private static final Pattern STRUCTURE = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    private static final String CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    // First two digits of a GSTIN = state code, per GSTN's published list.
    private static final Map<String, String> STATE_CODES = Map.ofEntries(
            Map.entry("01", "Jammu and Kashmir"), Map.entry("02", "Himachal Pradesh"),
            Map.entry("03", "Punjab"), Map.entry("04", "Chandigarh"),
            Map.entry("05", "Uttarakhand"), Map.entry("06", "Haryana"),
            Map.entry("07", "Delhi"), Map.entry("08", "Rajasthan"),
            Map.entry("09", "Uttar Pradesh"), Map.entry("10", "Bihar"),
            Map.entry("11", "Sikkim"), Map.entry("12", "Arunachal Pradesh"),
            Map.entry("13", "Nagaland"), Map.entry("14", "Manipur"),
            Map.entry("15", "Mizoram"), Map.entry("16", "Tripura"),
            Map.entry("17", "Meghalaya"), Map.entry("18", "Assam"),
            Map.entry("19", "West Bengal"), Map.entry("20", "Jharkhand"),
            Map.entry("21", "Odisha"), Map.entry("22", "Chhattisgarh"),
            Map.entry("23", "Madhya Pradesh"), Map.entry("24", "Gujarat"),
            Map.entry("26", "Dadra and Nagar Haveli and Daman and Diu"),
            Map.entry("27", "Maharashtra"), Map.entry("28", "Andhra Pradesh (Old)"),
            Map.entry("29", "Karnataka"), Map.entry("30", "Goa"),
            Map.entry("31", "Lakshadweep"), Map.entry("32", "Kerala"),
            Map.entry("33", "Tamil Nadu"), Map.entry("34", "Puducherry"),
            Map.entry("35", "Andaman and Nicobar Islands"), Map.entry("36", "Telangana"),
            Map.entry("37", "Andhra Pradesh"), Map.entry("38", "Ladakh"));

    public record GstDetails(boolean structureValid, boolean checksumValid, String stateCode, String stateName,
            String panNumber) {
        public boolean isFullyValid() {
            return structureValid && checksumValid;
        }
    }

    public static GstDetails parse(String rawGstin) {
        if (rawGstin == null) {
            return new GstDetails(false, false, null, null, null);
        }
        String gstin = rawGstin.trim().toUpperCase();

        boolean structureValid = STRUCTURE.matcher(gstin).matches();
        if (!structureValid) {
            return new GstDetails(false, false, null, null, null);
        }

        String stateCode = gstin.substring(0, 2);
        String stateName = STATE_CODES.get(stateCode);
        String pan = gstin.substring(2, 12);
        boolean checksumValid = stateName != null && checksumMatches(gstin);

        return new GstDetails(true, checksumValid, stateCode, stateName, pan);
    }

    /**
     * GSTIN's check digit is a Luhn-style mod-36 checksum over the first 14
     * characters, verified against the 15th. Alphabet is 0-9 then A-Z (base 36).
     */
    private static boolean checksumMatches(String gstin) {
        int factor = 2;
        int sum = 0;
        for (int i = 0; i < 14; i++) {
            int code = CODE_CHARS.indexOf(gstin.charAt(i));
            if (code < 0) return false;
            int product = code * factor;
            factor = (factor == 2) ? 1 : 2;
            product = (product / 36) + (product % 36);
            sum += product;
        }
        int checksumIndex = (36 - (sum % 36)) % 36;
        char expected = CODE_CHARS.charAt(checksumIndex);
        return expected == gstin.charAt(14);
    }
}
