/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Keith Donald
 */
class EmbeddedDatabaseFactoryBeanTests {

	private final ClassRelativeResourceLoader resourceLoader = new ClassRelativeResourceLoader(getClass());


	Resource resource(String path) {
		return resourceLoader.getResource(path);
	}

	@Test
	void factoryBeanLifecycle() {
		EmbeddedDatabaseFactoryBean bean = new EmbeddedDatabaseFactoryBean();
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource("db-schema.sql"),
			resource("db-test-data.sql"));
		bean.setDatabasePopulator(populator);
		bean.afterPropertiesSet();
		DataSource ds = bean.getObject();
		JdbcTemplate template = new JdbcTemplate(ds);
		assertThat(template.queryForObject("select NAME from T_TEST", String.class)).isEqualTo("Keith");
		bean.destroy();
	}

}
