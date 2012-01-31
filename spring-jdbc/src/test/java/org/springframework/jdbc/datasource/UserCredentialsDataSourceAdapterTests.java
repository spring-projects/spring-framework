/*
 * Copyright 2002-2005 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Juergen Hoeller
 * @since 28.05.2004
 */
public class UserCredentialsDataSourceAdapterTests extends TestCase {

	public void testStaticCredentials() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		ds.getConnection("user", "pw");
		dsControl.setReturnValue(con);
		dsControl.replay();
		conControl.replay();

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(ds);
		adapter.setUsername("user");
		adapter.setPassword("pw");
		assertEquals(con, adapter.getConnection());
	}

	public void testNoCredentials() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con);
		dsControl.replay();
		conControl.replay();

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(ds);
		assertEquals(con, adapter.getConnection());
	}

	public void testThreadBoundCredentials() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		ds.getConnection("user", "pw");
		dsControl.setReturnValue(con);
		dsControl.replay();
		conControl.replay();

		UserCredentialsDataSourceAdapter adapter = new UserCredentialsDataSourceAdapter();
		adapter.setTargetDataSource(ds);

		adapter.setCredentialsForCurrentThread("user", "pw");
		try {
			assertEquals(con, adapter.getConnection());
		}
		finally {
			adapter.removeCredentialsFromCurrentThread();
		}
	}

}
