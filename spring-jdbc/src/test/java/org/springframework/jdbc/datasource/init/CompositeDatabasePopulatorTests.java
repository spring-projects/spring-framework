/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import org.junit.After;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CompositeDatabasePopulator}.
 *
 * @author Kazuki Shimizu
 * @since 4.3
 */
public class CompositeDatabasePopulatorTests {

	private static final Connection mockedConnection = mock(Connection.class);
	private static final DatabasePopulator mockedDatabasePopulator1 = mock(DatabasePopulator.class);
	private static final DatabasePopulator mockedDatabasePopulator2 = mock(DatabasePopulator.class);

	@After
	public void resetMocked(){
		reset(mockedConnection, mockedDatabasePopulator1, mockedDatabasePopulator2);
	}

	@Test
	public void addPopulators() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.addPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);
		populator.populate(mockedConnection);
		verify(mockedDatabasePopulator1,times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	public void setPopulatorsWithMultiple() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2); // multiple
		populator.populate(mockedConnection);
		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	public void setPopulatorsForOverride() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1);
		populator.setPopulators(mockedDatabasePopulator2); // Override
		populator.populate(mockedConnection);
		verify(mockedDatabasePopulator1, times(0)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	public void constructWithMultiple() throws SQLException {
		CompositeDatabasePopulator populator =
				new CompositeDatabasePopulator(mockedDatabasePopulator1, mockedDatabasePopulator2);
		populator.populate(mockedConnection);
		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

}
