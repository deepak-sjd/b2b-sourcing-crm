package com.doctutorials.crm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class QuotationLockTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static String scalar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void sentQuotationContentIsLockedByTheDatabaseButStatusStillMoves() throws Exception {
        try (Connection c = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = c.createStatement()) {

            String org = scalar(st, "select id from organizations where slug = 'default'");
            UUID q = UUID.randomUUID();
            UUID item = UUID.randomUUID();
            String newItem = "insert into quotation_line_items (id, quotation_id, organization_id, product_name, "
                    + "quantity, selling_price) values ('" + UUID.randomUUID() + "', '" + q + "', '" + org
                    + "', 'Extra', 1, 10)";

            st.execute("set session_replication_role = replica"); // lets the test use a made-up lead id
            st.execute("insert into quotations (id, quotation_number, lead_id, organization_id) values ('"
                    + q + "', 'QT-LOCK-1', '" + UUID.randomUUID() + "', '" + org + "')");
            st.execute("set session_replication_role = origin");

            st.execute("insert into quotation_line_items (id, quotation_id, organization_id, product_name, "
                    + "quantity, selling_price) values ('" + item + "', '" + q + "', '" + org + "', 'Widget', 1, 100)");
            st.execute("update quotation_line_items set selling_price = 120 where id = '" + item + "'"); // DRAFT: ok
            st.execute("update quotations set status = 'SENT' where id = '" + q + "'");                  // status moves

            assertThatThrownBy(() -> st.execute("update quotations set remarks = 'changed' where id = '" + q + "'"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute("update quotation_line_items set selling_price = 1 where id = '" + item + "'"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute(newItem)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> st.execute("delete from quotation_line_items where id = '" + item + "'"))
                    .isInstanceOf(SQLException.class);

            st.execute("update quotations set superseded = true where id = '" + q + "'");
            st.execute("update quotations set status = 'VIEWED' where id = '" + q + "'");
        }
    }
}