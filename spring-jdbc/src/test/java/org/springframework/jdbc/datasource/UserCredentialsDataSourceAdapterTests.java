/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;

/**
 * @author Juergen Hoeller
 * @since 28.05.2004
 */
public class UserCredentialsDataSourceAdapterTests {

	@Test
	public void testStaticCredentials() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection("user", "pw")).willReturn(connection);

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);
		adapter.setUsername("user");
		adapter.setPassword("pw");
		assertEquals(connection, adapter.getConnection());
	}

	@Test
	public void testNoCredentials() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection()).willReturn(connection);
		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);
		assertEquals(connection, adapter.getConnection());
	}

	@Test
	public void testThreadBoundCredentials() throws SQLException {
		DataSource dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		given(dataSource.getConnection("user", "pw")).willReturn(connection);

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(dataSource);

		adapter.setCredentialsForCurrentThread("user", "pw");
		try {
			assertEquals(connection, adapter.getConnection());
		}
		finally {
			adapter.removeCredentialsFromCurrentThread();
		}
	}

}
