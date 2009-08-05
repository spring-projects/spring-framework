package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.DERBY;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

public class EmbeddedDatabaseBuilderTests {

	@Test
	public void testBuildDefaults() {
		EmbeddedDatabase db = EmbeddedDatabaseBuilder.buildDefault();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuild() {
		EmbeddedDatabaseBuilder builder = EmbeddedDatabaseBuilder.relativeTo(getClass());
		EmbeddedDatabase db = builder.script("db-schema.sql").script("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildH2() {
		EmbeddedDatabaseBuilder builder = EmbeddedDatabaseBuilder.relativeTo(getClass());
		EmbeddedDatabase db = builder.type(H2).script("db-schema.sql").script("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}


	public void testBuildDerby() {
		EmbeddedDatabaseBuilder builder = EmbeddedDatabaseBuilder.relativeTo(getClass());
		EmbeddedDatabase db = builder.type(DERBY).script("db-schema-derby.sql").script("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildNoSuchScript() {
		try {
			new EmbeddedDatabaseBuilder().script("bogus.sql").build();
			fail("Should have failed");
		} catch (CannotReadScriptException e) {
		}
	}

	private void assertDatabaseCreatedAndShutdown(EmbeddedDatabase db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		db.shutdown();
	}

}