/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.config;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

/**
 * @author Dave Syer
 */
public class JdbcNamespaceIntegrationTests {

	@Test
	public void testCreateEmbeddedDatabase() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config.xml");
		assertCorrectSetup(context, "dataSource", "h2DataSource", "derbyDataSource");
	}

	@Test
	public void testCreateEmbeddedDatabaseAgain() throws Exception {
		// If Derby isn't cleaned up properly this will fail...
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config.xml");
		assertCorrectSetup(context, "derbyDataSource");
	}

	@Test
	public void testCreateWithResourcePattern() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"org/springframework/jdbc/config/jdbc-config-pattern.xml");
		assertCorrectSetup(context, "dataSource");
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

		try {
			for (String dataSourceName : dataSources) {
				DataSource dataSource = context.getBean(dataSourceName, DataSource.class);
				JdbcTemplate t = new JdbcTemplate(dataSource);
				assertEquals(1, t.queryForInt("select count(*) from T_TEST"));
			}
		} finally {
			context.close();
		}
	}
}
