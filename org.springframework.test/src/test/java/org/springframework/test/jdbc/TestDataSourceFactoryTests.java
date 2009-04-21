package org.springframework.test.jdbc;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestDataSourceFactoryTests {
	TestDataSourceFactory factory = new TestDataSourceFactory();
	
	@Test
	public void testGetDataSource() {
		StubTestDataSourcePopulator populator = new StubTestDataSourcePopulator();
		factory.setDatabasePopulator(populator);
		factory.getDataSource();
		assertTrue(populator.populateCalled);
		factory.destroyDataSource();
	}
	
	private static class StubTestDataSourcePopulator implements TestDatabasePopulator {

		private boolean populateCalled;
		
		public void populate(JdbcTemplate template) throws DataAccessException {
			this.populateCalled = true;
		}
		
	}
}
