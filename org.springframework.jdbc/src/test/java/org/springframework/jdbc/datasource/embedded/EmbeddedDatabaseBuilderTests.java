package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

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
		EmbeddedDatabase db = builder.script("db-schema.sql").script("db-test-data.sql").build();
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

	@Test
	public void testBuildNoSuchScript() {
		try {
			new EmbeddedDatabaseBuilder().script("bogus.sql").build();
			fail("Should have failed");
		} catch (CannotReadScriptException e) {
		}
	}

}