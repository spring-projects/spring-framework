package org.springframework.jdbc.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcNamespaceIntegrationTest {

	@Test
	public void testCreateEmbeddedDatabase() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config.xml");
		assertCorrectSetup(context, "dataSource", "h2DataSource", "derbyDataSource");
		context.close();
	}

	@Test
	public void testCreateEmbeddedDatabaseAgain() throws Exception {
		// If Derby isn't cleaned up properly this will fail...
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config.xml");
		assertCorrectSetup(context, "derbyDataSource");
		context.close();
	}

	@Test
	public void testCreateWithResourcePattern() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config-pattern.xml");
		assertCorrectSetup(context, "dataSource");
		context.close();
	}

	@Test
	public void testCreateWithEndings() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-initialize-endings-config.xml");
		assertCorrectSetup(context, 2, "dataSource");
		context.close();
	}

	@Test
	public void testMultipleDataSourcesHaveDifferentDatabaseNames() throws Exception {

		DefaultListableBeanFactory factory = new XmlBeanFactory(new ClassPathResource(
				"org/springframework/jdbc/config/jdbc-config-multiple-datasources.xml"));

		assertBeanPropertyValueOf("databaseName", "firstDataSource", factory);
		assertBeanPropertyValueOf("databaseName", "secondDataSource", factory);
	}

	private void assertBeanPropertyValueOf(String propertyName, String expected, DefaultListableBeanFactory factory) {

		BeanDefinition bean = factory.getBeanDefinition(expected);
		PropertyValue value = bean.getPropertyValues().getPropertyValue(propertyName);
		assertThat(value, is(notNullValue()));
		assertThat(value.getValue().toString(), is(expected));
	}

	private void assertCorrectSetup(ConfigurableApplicationContext context, String... dataSources) {
		assertCorrectSetup(context, 1, dataSources);
	}
	
	private void assertCorrectSetup(ConfigurableApplicationContext context, int count, String... dataSources) {

		try {
			for (String dataSourceName : dataSources) {
				DataSource dataSource = context.getBean(dataSourceName, DataSource.class);
				JdbcTemplate t = new JdbcTemplate(dataSource);
				assertEquals(count, t.queryForInt("select count(*) from T_TEST"));
			}
		} finally {
			context.close();
		}

	}

}
