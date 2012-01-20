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

import java.sql.Connection;
import java.util.Properties;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Rod Johnson
 */
public class DriverManagerDataSourceTests extends TestCase {

	public void testStandardUsage() throws Exception {
		final String jdbcUrl = "url";
		final String uname = "uname";
		final String pwd = "pwd";

		MockControl ctrlConnection =
			MockControl.createControl(Connection.class);
		final Connection mockConnection = (Connection) ctrlConnection.getMock();
		ctrlConnection.replay();

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals(uname, props.getProperty("user"));
				assertEquals(pwd, props.getProperty("password"));
				return mockConnection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setUsername(uname);
		ds.setPassword(pwd);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == mockConnection);

		assertTrue(ds.getUrl().equals(jdbcUrl));
		assertTrue(ds.getPassword().equals(pwd));
		assertTrue(ds.getUsername().equals(uname));

		ctrlConnection.verify();
	}

	public void testUsageWithConnectionProperties() throws Exception {
		final String jdbcUrl = "url";

		final Properties connProps = new Properties();
		connProps.setProperty("myProp", "myValue");
		connProps.setProperty("yourProp", "yourValue");
		connProps.setProperty("user", "uname");
		connProps.setProperty("password", "pwd");

		MockControl ctrlConnection =
			MockControl.createControl(Connection.class);
		final Connection mockConnection = (Connection) ctrlConnection.getMock();
		ctrlConnection.replay();

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals("uname", props.getProperty("user"));
				assertEquals("pwd", props.getProperty("password"));
				assertEquals("myValue", props.getProperty("myProp"));
				assertEquals("yourValue", props.getProperty("yourProp"));
				return mockConnection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setConnectionProperties(connProps);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == mockConnection);

		assertTrue(ds.getUrl().equals(jdbcUrl));

		ctrlConnection.verify();
	}

	public void testUsageWithConnectionPropertiesAndUserCredentials() throws Exception {
		final String jdbcUrl = "url";
		final String uname = "uname";
		final String pwd = "pwd";

		final Properties connProps = new Properties();
		connProps.setProperty("myProp", "myValue");
		connProps.setProperty("yourProp", "yourValue");
		connProps.setProperty("user", "uname2");
		connProps.setProperty("password", "pwd2");

		MockControl ctrlConnection =
			MockControl.createControl(Connection.class);
		final Connection mockConnection = (Connection) ctrlConnection.getMock();
		ctrlConnection.replay();

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertEquals(jdbcUrl, url);
				assertEquals(uname, props.getProperty("user"));
				assertEquals(pwd, props.getProperty("password"));
				assertEquals("myValue", props.getProperty("myProp"));
				assertEquals("yourValue", props.getProperty("yourProp"));
				return mockConnection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setUsername(uname);
		ds.setPassword(pwd);
		ds.setConnectionProperties(connProps);

		Connection actualCon = ds.getConnection();
		assertTrue(actualCon == mockConnection);

		assertTrue(ds.getUrl().equals(jdbcUrl));
		assertTrue(ds.getPassword().equals(pwd));
		assertTrue(ds.getUsername().equals(uname));

		ctrlConnection.verify();
	}

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
