/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.config;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
class InitializeDatabaseIntegrationTests {

	private String enabled;

	private ClassPathXmlApplicationContext context;


	@BeforeEach
	void init() {
		enabled = System.setProperty("ENABLED", "true");
	}

	@AfterEach
	void after() {
		if (enabled != null) {
			System.setProperty("ENABLED", enabled);
		}
		else {
			System.clearProperty("ENABLED");
		}
		if (context != null) {
			context.close();
		}
	}


	@Test
	void testCreateEmbeddedDatabase() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-config.xml");
		assertCorrectSetup(context.getBean("dataSource", DataSource.class));
	}

	@Test
	void testDisableCreateEmbeddedDatabase() {
		System.setProperty("ENABLED", "false");
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-config.xml");
		assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
				assertCorrectSetup(context.getBean("dataSource", DataSource.class)));
	}

	@Test
	void testIgnoreFailedDrops() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-fail-config.xml");
		assertCorrectSetup(context.getBean("dataSource", DataSource.class));
	}

	@Test
	void testScriptNameWithPattern() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-pattern-config.xml");
		DataSource dataSource = context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
		JdbcTemplate t = new JdbcTemplate(dataSource);
		assertThat(t.queryForObject("select name from T_TEST", String.class)).isEqualTo("Dave");
	}

	@Test
	void testScriptNameWithPlaceholder() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-placeholder-config.xml");
		DataSource dataSource = context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
	}

	@Test
	void testScriptNameWithExpressions() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-expression-config.xml");
		DataSource dataSource = context.getBean("dataSource", DataSource.class);
		assertCorrectSetup(dataSource);
	}

	@Test
	void testCacheInitialization() {
		context = new ClassPathXmlApplicationContext("org/springframework/jdbc/config/jdbc-initialize-cache-config.xml");
		assertCorrectSetup(context.getBean("dataSource", DataSource.class));
		CacheData cache = context.getBean(CacheData.class);
		assertThat(cache.getCachedData()).hasSize(1);
	}

	private void assertCorrectSetup(DataSource dataSource) {
		JdbcTemplate jt = new JdbcTemplate(dataSource);
		assertThat(jt.queryForObject("select count(*) from T_TEST", Integer.class)).isEqualTo(1);
	}


	public static class CacheData implements InitializingBean {

		private JdbcTemplate jdbcTemplate;

		private List<Map<String,Object>> cache;

		public void setDataSource(DataSource dataSource) {
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		public List<Map<String,Object>> getCachedData() {
			return cache;
		}

		@Override
		public void afterPropertiesSet() {
			cache = jdbcTemplate.queryForList("SELECT * FROM T_TEST");
		}
	}

}
