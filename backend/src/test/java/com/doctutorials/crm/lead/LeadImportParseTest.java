package com.doctutorials.crm.lead;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeadImportParseTest {

    @Test
    void parsesQuotesCommasNewlinesBomAndCrlf() {
        String csv = "\uFEFFname,phone,interest\r\n"
                + "\"Rao, Asha\",9876543210,\"Premium \"\"Plus\"\" plan\"\r\n"
                + "Ravi,,\"line1\nline2\"\r\n";
        List<List<String>> rows = LeadImportService.parseCsv(csv);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)).containsExactly("name", "phone", "interest");
        assertThat(rows.get(1)).containsExactly("Rao, Asha", "9876543210", "Premium \"Plus\" plan");
        assertThat(rows.get(2)).containsExactly("Ravi", "", "line1\nline2");
    }
}