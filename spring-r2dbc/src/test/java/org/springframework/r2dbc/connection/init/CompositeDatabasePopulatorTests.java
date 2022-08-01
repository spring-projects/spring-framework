/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.init;

import java.util.LinkedHashSet;
import java.util.Set;

import io.r2dbc.spi.Connection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;

/**
 * Unit tests for {@link CompositeDatabasePopulator}.
 *
 * @author Kazuki Shimizu
 * @author Juergen Hoeller
 * @author Mark Paluch
 */
class CompositeDatabasePopulatorTests {

	Connection mockedConnection = mock(Connection.class);

	DatabasePopulator mockedDatabasePopulator1 = mock(DatabasePopulator.class);

	DatabasePopulator mockedDatabasePopulator2 = mock(DatabasePopulator.class);


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
