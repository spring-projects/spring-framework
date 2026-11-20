/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CompositeDatabasePopulator}.
 *
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 * @since 4.3
 */
class CompositeDatabasePopulatorTests {

	private final Connection mockedConnection = mock();

	private final DatabasePopulator mockedDatabasePopulator1 = mock();

	private final DatabasePopulator mockedDatabasePopulator2 = mock();


	@Test
	void addPopulators() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.addPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);

		populator.populate(mockedConnection);

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void setPopulatorsWithMultiple() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);  // multiple

		populator.populate(mockedConnection);

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void setPopulatorsForOverride() throws SQLException {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1);
		populator.setPopulators(mockedDatabasePopulator2);  // override

		populator.populate(mockedConnection);

		verify(mockedDatabasePopulator1, times(0)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void constructWithVarargs() throws SQLException {
		CompositeDatabasePopulator populator =
				new CompositeDatabasePopulator(mockedDatabasePopulator1, mockedDatabasePopulator2);

		populator.populate(mockedConnection);

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void constructWithCollection() throws SQLException {
		Set<DatabasePopulator> populators = new LinkedHashSet<>();
		populators.add(mockedDatabasePopulator1);
		populators.add(mockedDatabasePopulator2);

		CompositeDatabasePopulator populator = new CompositeDatabasePopulator(populators);
		populator.populate(mockedConnection);

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

}
