package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.DERBY;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType.H2;

import org.junit.Test;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;

public class EmbeddedDatabaseBuilderTests {

	@Test
	public void testBuildDefaultScripts() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder.addDefaultScripts().build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuild() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.addScript("db-schema.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildWithComments() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.addScript("db-schema-comments.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildH2() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.setType(H2).addScript("db-schema.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildDerby() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder(new ClassRelativeResourceLoader(getClass()));
		EmbeddedDatabase db = builder.setType(DERBY).addScript("db-schema-derby.sql").addScript("db-test-data.sql").build();
		assertDatabaseCreatedAndShutdown(db);
	}

	@Test
	public void testBuildNoSuchScript() {
		try {
			new EmbeddedDatabaseBuilder().addScript("bogus.sql").build();
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