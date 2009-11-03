package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class EmbeddedDatabasesTests {

	@Test
	public void testCreateDefault() {
		EmbeddedDatabase db = EmbeddedDatabases.createDefault();
		assertDatabaseCreatedAndShutdown(db);
	}

	private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

}