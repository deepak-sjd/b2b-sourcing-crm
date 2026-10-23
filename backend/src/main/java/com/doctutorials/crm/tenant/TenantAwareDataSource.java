package com.doctutorials.crm.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Wraps the pool. Every time a connection is handed out it is told which tenant it
 * serves (session variable app.tenant_id), which Postgres row-level security policies
 * read. The value is overwritten on every borrow, so a pooled connection can never
 * carry a previous tenant's value.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return announceTenant(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return announceTenant(super.getConnection(username, password));
    }

    private Connection announceTenant(Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select set_config('app.tenant_id', ?, false)")) {
            ps.setString(1, TenantContext.currentOrSystem().toString());
            ps.execute();
            return connection;
        } catch (SQLException | RuntimeException e) {
            connection.close();
            throw e;
        }
    }
}