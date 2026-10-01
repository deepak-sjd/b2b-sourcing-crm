package com.doctutorials.crm.customer;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI automation #2 — the first two digits of an Indian GSTIN are a fixed,
 * government-assigned state code. This is a lookup table, not an inference,
 * so "auto-fill state from GST number" is fully reliable for a well-formed
 * GSTIN — there's no probability score to hide behind. The frontend only
 * uses this to pre-fill the billing address's State field when it's still
 * empty; the user can always overwrite it.
 */
@Service
public class GstStateResolver {

    private static final Map<String, String> STATE_CODES = Map.ofEntries(
            Map.entry("01", "Jammu and Kashmir"),
            Map.entry("02", "Himachal Pradesh"),
            Map.entry("03", "Punjab"),
            Map.entry("04", "Chandigarh"),
            Map.entry("05", "Uttarakhand"),
            Map.entry("06", "Haryana"),
            Map.entry("07", "Delhi"),
            Map.entry("08", "Rajasthan"),
            Map.entry("09", "Uttar Pradesh"),
            Map.entry("10", "Bihar"),
            Map.entry("11", "Sikkim"),
            Map.entry("12", "Arunachal Pradesh"),
            Map.entry("13", "Nagaland"),
            Map.entry("14", "Manipur"),
            Map.entry("15", "Mizoram"),
            Map.entry("16", "Tripura"),
            Map.entry("17", "Meghalaya"),
            Map.entry("18", "Assam"),
            Map.entry("19", "West Bengal"),
            Map.entry("20", "Jharkhand"),
            Map.entry("21", "Odisha"),
            Map.entry("22", "Chhattisgarh"),
            Map.entry("23", "Madhya Pradesh"),
            Map.entry("24", "Gujarat"),
            Map.entry("25", "Daman and Diu"),
            Map.entry("26", "Dadra and Nagar Haveli"),
            Map.entry("27", "Maharashtra"),
            Map.entry("28", "Andhra Pradesh (Old)"),
            Map.entry("29", "Karnataka"),
            Map.entry("30", "Goa"),
            Map.entry("31", "Lakshadweep"),
            Map.entry("32", "Kerala"),
            Map.entry("33", "Tamil Nadu"),
            Map.entry("34", "Puducherry"),
            Map.entry("35", "Andaman and Nicobar Islands"),
            Map.entry("36", "Telangana"),
            Map.entry("37", "Andhra Pradesh"),
            Map.entry("38", "Ladakh"),
            Map.entry("97", "Other Territory")
    );

    public record GstInfo(boolean valid, String stateCode, String state) {}

    public GstInfo resolve(String gstNo) {
        if (gstNo == null || gstNo.trim().length() < 2) {
            return new GstInfo(false, null, null);
        }
        String code = gstNo.trim().substring(0, 2);
        String state = STATE_CODES.get(code);
        return new GstInfo(state != null, code, state);
    }
}
