/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.datasource;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.util.Properties;

import org.junit.Test;

/**
 * @author Rod Johnson
 */
public class DriverManagerDataSourceTests {

	private Connection connection = mock(Connection.class);

	@Test
	public void testStandardUsage() throws Exception {
		final String jdbcUrl = "url";
		final String uname = "uname";
		final String pwd = "pwd";

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			@Override
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals(uname, props.getProperty("user"));
				assertEquals(pwd, props.getProperty("password"));
				return connection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setUsername(uname);
		ds.setPassword(pwd);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == connection);

		assertTrue(ds.getUrl().equals(jdbcUrl));
		assertTrue(ds.getPassword().equals(pwd));
		assertTrue(ds.getUsername().equals(uname));
	}

	@Test
	public void testUsageWithConnectionProperties() throws Exception {
		final String jdbcUrl = "url";

		final Properties connProps = new Properties();
		connProps.setProperty("myProp", "myValue");
		connProps.setProperty("yourProp", "yourValue");
		connProps.setProperty("user", "uname");
		connProps.setProperty("password", "pwd");

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			@Override
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals("uname", props.getProperty("user"));
				assertEquals("pwd", props.getProperty("password"));
				assertEquals("myValue", props.getProperty("myProp"));
				assertEquals("yourValue", props.getProperty("yourProp"));
				return connection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setConnectionProperties(connProps);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == connection);

		assertTrue(ds.getUrl().equals(jdbcUrl));
	}

	@Test
	public void testUsageWithConnectionPropertiesAndUserCredentials() throws Exception {
		final String jdbcUrl = "url";
		final String uname = "uname";
		final String pwd = "pwd";

		final Properties connProps = new Properties();
		connProps.setProperty("myProp", "myValue");
		connProps.setProperty("yourProp", "yourValue");
		connProps.setProperty("user", "uname2");
		connProps.setProperty("password", "pwd2");

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			@Override
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals(uname, props.getProperty("user"));
				assertEquals(pwd, props.getProperty("password"));
				assertEquals("myValue", props.getProperty("myProp"));
				assertEquals("yourValue", props.getProperty("yourProp"));
				return connection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setUsername(uname);
		ds.setPassword(pwd);
		ds.setConnectionProperties(connProps);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == connection);

		assertTrue(ds.getUrl().equals(jdbcUrl));
		assertTrue(ds.getPassword().equals(pwd));
		assertTrue(ds.getUsername().equals(uname));
	}

	@Test
	public void testInvalidClassName() throws Exception {
		String bogusClassName = "foobar";
		DriverManagerDataSource ds = new DriverManagerDataSource();
		try {
			ds.setDriverClassName(bogusClassName);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			// OK
			assertTrue(ex.getCause() instanceof ClassNotFoundException);
		}
	}

}
