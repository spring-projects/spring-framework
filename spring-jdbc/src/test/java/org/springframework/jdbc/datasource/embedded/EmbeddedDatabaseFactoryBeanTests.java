package org.springframework.jdbc.datasource.embedded;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class EmbeddedDatabaseFactoryBeanTests {

	@Test
	public void testFactoryBeanLifecycle() throws Exception {
		EmbeddedDatabaseFactoryBean bean = new EmbeddedDatabaseFactoryBean();
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.setScripts(new Resource[] {
			new ClassPathResource("db-schema.sql", getClass()),
			new ClassPathResource("db-test-data.sql", getClass())
		});
		bean.setDatabasePopulator(populator);
		bean.afterPropertiesSet();
		DataSource ds = bean.getObject();
		JdbcTemplate template = new JdbcTemplate(ds);
		assertEquals("Keith", template.queryForObject("select NAME from T_TEST", String.class));
		bean.destroy();
	}
}
