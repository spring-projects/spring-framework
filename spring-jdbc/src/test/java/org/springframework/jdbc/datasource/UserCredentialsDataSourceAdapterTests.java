/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @since 28.05.2004
 */
class UserCredentialsDataSourceAdapterTests {

	@Test
	void staticCredentials() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		given(dataSource.getConnection("user", "pw")).willReturn(connection);

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);
		adapter.setUsername("user");
		adapter.setPassword("pw");
		assertThat(adapter.getConnection()).isEqualTo(connection);
	}

	@Test
	void noCredentials() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		given(dataSource.getConnection()).willReturn(connection);
		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);
		assertThat(adapter.getConnection()).isEqualTo(connection);
	}

	@Test
	void threadBoundCredentials() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		given(dataSource.getConnection("user", "pw")).willReturn(connection);

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);

		adapter.setCredentialsForCurrentThread("user", "pw");
		try {
			assertThat(adapter.getConnection()).isEqualTo(connection);
		}
		finally {
			adapter.removeCredentialsFromCurrentThread();
		}
	}

}
