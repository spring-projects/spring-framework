package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

public class EmbeddedDatabaseBuilderTests {
	
	@Test
	public void testBuildDefaults() {
		EmbeddedDatabase db = EmbeddedDatabaseBuilder.buildDefault();
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}
	
	@Test
	public void testBuild() {
		EmbeddedDatabaseBuilder builder = EmbeddedDatabaseBuilder.relativeTo(getClass());
		EmbeddedDatabase db = builder.schema("db-schema.sql").testData("db-test-data.sql").build();
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

	@Test
	public void testBuildNoSuchSchema() {
		try {
			new EmbeddedDatabaseBuilder().schema("bogus.sql").build();
			fail("Should have failed");
		} catch (DataAccessException e) {
			
		}
	}
	
	@Test
	public void testBuildNoSuchTestdata() {
		try {
			new EmbeddedDatabaseBuilder().testData("bogus.sql").build();
			fail("Should have failed");
		} catch (DataAccessException e) {
			
		}
	}


}
