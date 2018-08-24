package org.springframework.jdbc.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TestDataSourceWrapper extends AbstractDataSource {

	private DataSource target;

	public void setTarget(DataSource target) {
		this.target = target;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return target.getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return target.getConnection(username, password);
	}

}
