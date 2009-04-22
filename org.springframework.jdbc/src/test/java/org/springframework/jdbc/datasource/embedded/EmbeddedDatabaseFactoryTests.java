package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.DatabasePopulator;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;

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
		
		public void populate(JdbcTemplate template) throws DataAccessException {
			this.populateCalled = true;
		}
		
	}
}
