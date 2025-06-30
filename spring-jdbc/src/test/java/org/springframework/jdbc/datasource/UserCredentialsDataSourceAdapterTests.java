/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	void testStaticCredentials() throws SQLException {
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
	void testNoCredentials() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		given(dataSource.getConnection()).willReturn(connection);
		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);
		assertThat(adapter.getConnection()).isEqualTo(connection);
	}

	@Test
	void testThreadBoundCredentials() throws SQLException {
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
