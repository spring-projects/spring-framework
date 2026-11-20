/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.config;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.testfixture.EnabledForTestGroups;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean;
import org.springframework.jdbc.datasource.init.CannotReadScriptException;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.core.testfixture.TestGroup.LONG_RUNNING;
import static org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory.DEFAULT_DATABASE_NAME;

/**
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Stephane Nicoll
 */
class JdbcNamespaceIntegrationTests {

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void createEmbeddedDatabase() {
		assertCorrectSetup("jdbc-config.xml", "dataSource", "h2DataSource", "derbyDataSource");
	}

	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	void createEmbeddedDatabaseAgain() {
		// If Derby isn't cleaned up properly this will fail...
		assertCorrectSetup("jdbc-config.xml", "derbyDataSource");
	}

	@Test
	void createWithResourcePattern() {
		assertCorrectSetup("jdbc-config-pattern.xml", "dataSource");
	}

	@Test
	void createWithNonExistentResource() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> assertCorrectSetup("jdbc-config-nonexistent.xml", "dataSource"))
				.withCauseInstanceOf(CannotReadScriptException.class);
	}

	@Test
	void createWithAnonymousDataSourceAndDefaultDatabaseName() {
		assertThat(extractDataSourceUrl("jdbc-config-db-name-default-and-anonymous-datasource.xml"))
				.endsWith(DEFAULT_DATABASE_NAME);
	}

	@Test
	void createWithImplicitDatabaseName() {
		assertThat(extractDataSourceUrl("jdbc-config-db-name-implicit.xml")).endsWith("dataSource");
	}

	@Test
	void createWithExplicitDatabaseName() {
		assertThat(extractDataSourceUrl("jdbc-config-db-name-explicit.xml")).endsWith("customDbName");
	}

	@Test
	void createWithGeneratedDatabaseName() {
		assertThat(extractDataSourceUrl("jdbc-config-db-name-generated.xml")).startsWith("jdbc:hsqldb:mem:")
				.doesNotEndWith("dataSource").doesNotEndWith("shouldBeOverriddenByGeneratedName");
	}

	@Test
	void createWithEndings() {
		assertCorrectSetupAndCloseContext("jdbc-initialize-endings-config.xml", 2, "dataSource");
	}

	@Test
	void createWithEndingsNested() {
		assertCorrectSetupAndCloseContext("jdbc-initialize-endings-nested-config.xml", 2, "dataSource");
	}

	@Test
	void createAndDestroy() {
		try (ClassPathXmlApplicationContext context = context("jdbc-destroy-config.xml")) {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(DataSourceInitializer.class).destroy();
			// Table has been dropped
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
					assertNumRowsInTestTable(template, 1));
		}
	}

	@Test
	void createAndDestroyNestedWithHsql() {
		try (ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config.xml")) {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
			// Table has been dropped
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
					assertNumRowsInTestTable(template, 1));
		}
	}

	@Test
	void createAndDestroyNestedWithH2() {
		try (ClassPathXmlApplicationContext context = context("jdbc-destroy-nested-config-h2.xml")) {
			DataSource dataSource = context.getBean(DataSource.class);
			JdbcTemplate template = new JdbcTemplate(dataSource);
			assertNumRowsInTestTable(template, 1);
			context.getBean(EmbeddedDatabaseFactoryBean.class).destroy();
			 // Table has been dropped
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(() ->
					assertNumRowsInTestTable(template, 1));
		}
	}

	@Test
	void multipleDataSourcesHaveDifferentDatabaseNames() {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(factory).loadBeanDefinitions(new ClassPathResource(
			"jdbc-config-multiple-datasources.xml", getClass()));
		assertBeanPropertyValueOf("databaseName", "firstDataSource", factory);
		assertBeanPropertyValueOf("databaseName", "secondDataSource", factory);
	}

	@Test
	void initializeWithCustomSeparator() {
		assertCorrectSetupAndCloseContext("jdbc-initialize-custom-separator.xml", 2, "dataSource");
	}

	@Test
	void embeddedWithCustomSeparator() {
		assertCorrectSetupAndCloseContext("jdbc-config-custom-separator.xml", 2, "dataSource");
	}

	private ClassPathXmlApplicationContext context(String file) {
		return new ClassPathXmlApplicationContext(file, getClass());
	}

	private void assertBeanPropertyValueOf(String propertyName, String expected, DefaultListableBeanFactory factory) {
		BeanDefinition bean = factory.getBeanDefinition(expected);
		PropertyValue value = bean.getPropertyValues().getPropertyValue(propertyName);
		assertThat(value).isNotNull();
		assertThat(value.getValue().toString()).isEqualTo(expected);
	}

	private void assertNumRowsInTestTable(JdbcTemplate template, int count) {
		assertThat(template.queryForObject("select count(*) from T_TEST", Integer.class)).isEqualTo(count);
	}

	private void assertCorrectSetup(String file, String... dataSources) {
		assertCorrectSetupAndCloseContext(file, 1, dataSources);
	}

	private void assertCorrectSetupAndCloseContext(String file, int count, String... dataSources) {
		try (ConfigurableApplicationContext context = context(file)) {
			for (String dataSourceName : dataSources) {
				DataSource dataSource = context.getBean(dataSourceName, DataSource.class);
				assertNumRowsInTestTable(new JdbcTemplate(dataSource), count);
				assertThat(dataSource).isInstanceOf(AbstractDriverBasedDataSource.class);
				AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
				assertThat(adbDataSource.getUrl()).contains(dataSourceName);
			}
		}
	}

	private @Nullable String extractDataSourceUrl(String file) {
		try (ConfigurableApplicationContext context = context(file)) {
			DataSource dataSource = context.getBean(DataSource.class);
			assertNumRowsInTestTable(new JdbcTemplate(dataSource), 1);
			assertThat(dataSource).isInstanceOf(AbstractDriverBasedDataSource.class);
			AbstractDriverBasedDataSource adbDataSource = (AbstractDriverBasedDataSource) dataSource;
			return adbDataSource.getUrl();
		}
	}

}
