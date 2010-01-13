package org.springframework.jdbc.datasource.init;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

public class DatabasePopulatorTests {

	@Test
	public void testBuildWithCommentsAndFailedDrop() throws Exception {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder.build();
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());
		databasePopulator.addScript(resourceLoader.getResource("db-schema-failed-drop-comments.sql"));
		databasePopulator.addScript(resourceLoader.getResource("db-test-data.sql"));
		databasePopulator.setIgnoreFailedDrops(true);
		Connection connection = db.getConnection();
		try {
			databasePopulator.populate(connection);
		} finally {
			connection.close();
		}
		assertDatabaseCreated(db);
		db.shutdown();
	}

	private void assertDatabaseCreated(DataSource db) {
		JdbcTemplate template = new JdbcTemplate(db);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
	}

}