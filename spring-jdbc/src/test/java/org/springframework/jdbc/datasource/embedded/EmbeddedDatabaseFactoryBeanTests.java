/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassRelativeResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptStatementFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

	@Test
	void shutdownDatabaseWhenDatabaseCleanerFails() {
		EmbeddedDatabaseFactoryBean bean = new EmbeddedDatabaseFactoryBean();
		bean.setDatabaseName("cleanerFailure");
		bean.setDatabasePopulator(new ResourceDatabasePopulator(resource("db-schema.sql")));
		bean.setDatabaseCleaner(new ResourceDatabasePopulator(
				new ByteArrayResource("drop table T_MISSING".getBytes(), "inline SQL")));
		bean.afterPropertiesSet();
		assertThatExceptionOfType(ScriptStatementFailedException.class).isThrownBy(bean::destroy);

		EmbeddedDatabaseFactoryBean next = new EmbeddedDatabaseFactoryBean();
		next.setDatabaseName("cleanerFailure");
		next.afterPropertiesSet();
		JdbcTemplate template = new JdbcTemplate(next.getObject());
		assertThat(template.queryForObject(
				"select count(*) from INFORMATION_SCHEMA.TABLES where TABLE_NAME = 'T_TEST'", Integer.class))
				.isZero();
		next.destroy();
	}

}
