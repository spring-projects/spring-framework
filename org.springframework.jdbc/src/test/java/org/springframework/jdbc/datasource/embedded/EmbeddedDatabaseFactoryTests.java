package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertTrue;

import java.sql.Connection;

import org.junit.Test;
import org.springframework.jdbc.datasource.init.DatabasePopulator;

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
		
		public void populate(Connection connection) {
			this.populateCalled = true;
		}
		
	}
}
