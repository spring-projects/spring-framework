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

package org.springframework.jdbc.core.simple;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashMap;

/**
 * Mock object based tests for SimpleJdbcInsert.
 *
 * @author Thomas Risberg
 */
public class SimpleJdbcInsertTests extends TestCase {

	private MockControl ctrlDataSource;
	private DataSource mockDataSource;
	private MockControl ctrlConnection;
	private Connection mockConnection;
	private MockControl ctrlDatabaseMetaData;
	private DatabaseMetaData mockDatabaseMetaData;

	protected void setUp() throws Exception {
		super.setUp();

		ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(mockDatabaseMetaData);
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setDefaultReturnValue(mockConnection);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ctrlDatabaseMetaData.verify();
		ctrlDataSource.verify();
	}

	protected void replay() {
		ctrlDatabaseMetaData.replay();
		ctrlConnection.replay();
		ctrlDataSource.replay();
	}

	public void testNoSuchTable() throws Exception {
		final String NO_SUCH_TABLE = "x";
		final String USER = "me";

		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.supportsGetGeneratedKeys();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getDatabaseProductVersion();
		ctrlDatabaseMetaData.setReturnValue("1.0");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue(USER);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);
		mockDatabaseMetaData.getTables(null, null, NO_SUCH_TABLE, null);
		ctrlDatabaseMetaData.setReturnValue(mockResultSet);

		ctrlResultSet.replay();
		replay();

		SimpleJdbcInsert insert = new SimpleJdbcInsert(mockDataSource).withTableName(NO_SUCH_TABLE);
		try {
			insert.execute(new HashMap<String, Object>());
			fail("Shouldn't succeed in inserting into table which doesn't exist");
		} catch (InvalidDataAccessApiUsageException ex) {
			// OK
		}
	}

	public void testInsert() throws Exception {
		replay();
	}
}
