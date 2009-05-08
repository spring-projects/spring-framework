package org.springframework.jdbc.config;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcNamespaceIntegrationTest {

	@Test
	public void testCreateEmbeddedDatabase() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-config.xml");
		DataSource ds = context.getBean("dataSource", DataSource.class);
		JdbcTemplate t = new JdbcTemplate(ds);
		assertEquals(1, t.queryForInt("select count(*) from T_TEST"));
	}
}
