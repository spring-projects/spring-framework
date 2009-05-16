package org.springframework.jdbc.config;

import static org.junit.Assert.*;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcNamespaceIntegrationTest {

	@Test
	public void testCreateEmbeddedDatabase() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config.xml");
		assertCorrectSetup(context.getBean("dataSource", DataSource.class));
		assertCorrectSetup(context.getBean("h2DataSource", DataSource.class));
		assertCorrectSetup(context.getBean("derbyDataSource", DataSource.class));
		context.close();
	}

	private void assertCorrectSetup(DataSource dataSource) {
		JdbcTemplate t = new JdbcTemplate(dataSource);
		assertEquals(1, t.queryForInt("select count(*) from T_TEST"));
	}
}
