/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Connection;

import org.junit.Test;

import org.springframework.jdbc.datasource.init.DatabasePopulator;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 */
public class EmbeddedDatabaseFactoryTests {

	private EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();


	@Test
	public void testGetDataSource() {
		StubDatabasePopulator populator = new StubDatabasePopulator();
		factory.setDatabasePopulator(populator);
		EmbeddedDatabase db = factory.getDatabase();
		assertTrue(populator.populateCalled);
		db.shutdown();
	}


	private static class StubDatabasePopulator implements DatabasePopulator {

		private boolean populateCalled;

		@Override
		public void populate(Connection connection) {
			this.populateCalled = true;
		}
	}

}
