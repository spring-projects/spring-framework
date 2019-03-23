/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.function.Predicate;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory.*;

/**
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
public class JdbcNamespaceIntegrationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();


	@Test
	public void createEmbeddedDatabase() throws Exception {
		Assume.group(TestGroup.LONG_RUNNING);
		assertCorrectSetup("jdbc-config.xml", "dataSource", "h2DataSource", "derbyDataSource");
	}

	@Test
	public void createEmbeddedDatabaseAgain() throws Exception {
		// If Derby isn't cleaned up properly this will fail...
		Assume.group(TestGroup.LONG_RUNNING);
		assertCorrectSetup("jdbc-config.xml", "derbyDataSource");
	}

	@Test
	public void createWithResourcePattern() throws Exception {
		assertCorrectSetup("jdbc-config-pattern.xml", "dataSource");
	}

	@Test
	public void createWithAnonymousDataSourceAndDefaultDatabaseName() throws Exception {
		assertCorrectSetupForSingleDataSource("jdbc-config-db-name-default-and-anonymous-datasource.xml",
			(url) -> url.endsWith(DEFAULT_DATABASE_NAME));
	}

	@Test
	public void createWithImplicitDatabaseName() throws Exception {
		assertCorrectSetupForSingleDataSource("jdbc-config-db-name-implicit.xml", (url) -> url.endsWith("dataSource"));
	}

	@Test
	public void createWithExplicitDatabaseName() throws Exception {
		assertCorrectSetupForSingleDataSource("jdbc-config-db-name-explicit.xml", (url) -> url.endsWith("customDbName"));
	}

	@Test
	public void createWithGeneratedDatabaseName() throws Exception {
		Predicate<String> urlPredicate = (url) -> url.startsWith("jdbc:hsqldb:mem:");
		urlPredicate.and((url) -> !url.endsWith("dataSource"));
		urlPredicate.and((url) -> !url.endsWith("shouldBeOverriddenByGeneratedName"));

		assertCorrectSetupForSingleDataSource("jdbc-config-db-name-generated.xml", urlPredicate);
	}

	@Test
	public void createWithEndings() throws Exception {
		assertCorrectSetupAndCloseContext("jdbc-initialize-endings-config.xml", 2, "dataSource");
	}

	@Test
	public void createWithEndingsNested() throws Exception {
		assertCorrectSetupAndCloseContext("jdbc-initialize-endings-nested-config.xml", 2, "dataSource");
	}

	@Test
	public void createAndDestroy() throws Exception {
		ClassPathXmlApplicationContext context = context("jdbc-destroy-config.xml");
		try {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(DataSourceInitializer.class).destroy();
			expected.expect(BadSqlGrammarException.class); // Table has been dropped
			assertNumRowsInTestTable(template, 1);
		}
		finally {
			context.close();
		}
	}

	@Test
	public void createAndDestroyNestedWithHsql() throws Exception {
		ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config.xml");
		try {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
			expected.expect(BadSqlGrammarException.class); // Table has been dropped
			assertNumRowsInTestTable(template, 1);
		}
		finally {
			context.close();
		}
	}

	@Test
	public void createAndDestroyNestedWithH2() throws Exception {
		ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config-h2.xml");
		try {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
			expected.expect(BadSqlGrammarException.class); // Table has been dropped
			assertNumRowsInTestTable(template, 1);
		}
		finally {
			context.close();
		}
	}

	@Test
	public void multipleDataSourcesHaveDifferentDatabaseNames() throws Exception {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ClassPathResource(
			"jdbc-config-multiple-datasources.xml", getClass()));
		assertBeanPropertyValueOf("databaseName", "firstDataSource", factory);
		assertBeanPropertyValueOf("databaseName", "secondDataSource", factory);
	}

	@Test
	public void initializeWithCustomSeparator() throws Exception {
		assertCorrectSetupAndCloseContext("jdbc-initialize-custom-separator.xml", 2, "dataSource");
	}

	@Test
	public void embeddedWithCustomSeparator() throws Exception {
		assertCorrectSetupAndCloseContext("jdbc-config-custom-separator.xml", 2, "dataSource");
	}

	private ClassPathXmlApplicationContext context(String file) {
		return new ClassPathXmlApplicationContext(file, getClass());
	}

	private void assertBeanPropertyValueOf(String propertyName, String expected, DefaultListableBeanFactory factory) {
		BeanDefinition bean = factory.getBeanDefinition(expected);
		PropertyValue value = bean.getPropertyValues().getPropertyValue(propertyName);
		assertThat(value, is(notNullValue()));
		assertThat(value.getValue().toString(), is(expected));
	}

	private void assertNumRowsInTestTable(JdbcTemplate template, int count) {
		assertEquals(count, template.queryForObject("select count(*) from T_TEST", Integer.class).intValue());
	}

	private void assertCorrectSetup(String file, String... dataSources) {
		assertCorrectSetupAndCloseContext(file, 1, dataSources);
	}

	private void assertCorrectSetupAndCloseContext(String file, int count, String... dataSources) {
		ConfigurableApplicationContext context = context(file);
		try {
			for (String dataSourceName : dataSources) {
				DataSource dataSource = context.getBean(dataSourceName, DataSource.class);
				assertNumRowsInTestTable(new JdbcTemplate(dataSource), count);
				assertTrue(dataSource instanceof AbstractDriverBasedDataSource);
				AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
				assertThat(adbDataSource.getUrl(), containsString(dataSourceName));
			}
		}
		finally {
			context.close();
		}
	}

	private void assertCorrectSetupForSingleDataSource(String file, Predicate<String> urlPredicate) {
		ConfigurableApplicationContext context = context(file);
		try {
			DataSource dataSource = context.getBean(DataSource.class);
			assertNumRowsInTestTable(new JdbcTemplate(dataSource), 1);
			assertTrue(dataSource instanceof AbstractDriverBasedDataSource);
			AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
			assertTrue(urlPredicate.test(adbDataSource.getUrl()));
		}
		finally {
			context.close();
		}
	}

}
