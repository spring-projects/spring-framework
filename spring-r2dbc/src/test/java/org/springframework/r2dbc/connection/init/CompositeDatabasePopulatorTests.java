/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.init;

import java.util.LinkedHashSet;
import java.util.Set;

import io.r2dbc.spi.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CompositeDatabasePopulator}.
 *
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 * @author Mark Paluch
 */
class CompositeDatabasePopulatorTests {

	Connection mockedConnection = mock();

	DatabasePopulator mockedDatabasePopulator1 = mock();

	DatabasePopulator mockedDatabasePopulator2 = mock();


	@BeforeEach
	void before() {
		when(mockedDatabasePopulator1.populate(mockedConnection)).thenReturn(Mono.empty());
		when(mockedDatabasePopulator2.populate(mockedConnection)).thenReturn(Mono.empty());
	}


	@Test
	void addPopulators() {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.addPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2);

		populator.populate(mockedConnection).as(StepVerifier::create).verifyComplete();

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void setPopulatorsWithMultiple() {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1, mockedDatabasePopulator2); // multiple

		populator.populate(mockedConnection).as(StepVerifier::create).verifyComplete();

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void setPopulatorsForOverride() {
		CompositeDatabasePopulator populator = new CompositeDatabasePopulator();
		populator.setPopulators(mockedDatabasePopulator1);
		populator.setPopulators(mockedDatabasePopulator2); // override

		populator.populate(mockedConnection).as(StepVerifier::create).verifyComplete();

		verify(mockedDatabasePopulator1, times(0)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void constructWithVarargs() {
		CompositeDatabasePopulator populator =
				new CompositeDatabasePopulator(mockedDatabasePopulator1, mockedDatabasePopulator2);

		populator.populate(mockedConnection).as(StepVerifier::create).verifyComplete();

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

	@Test
	void constructWithCollection() {
		Set<DatabasePopulator> populators = new LinkedHashSet<>();
		populators.add(mockedDatabasePopulator1);
		populators.add(mockedDatabasePopulator2);

		CompositeDatabasePopulator populator = new CompositeDatabasePopulator(populators);
		populator.populate(mockedConnection).as(StepVerifier::create).verifyComplete();

		verify(mockedDatabasePopulator1, times(1)).populate(mockedConnection);
		verify(mockedDatabasePopulator2, times(1)).populate(mockedConnection);
	}

}
