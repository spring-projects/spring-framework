/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * @author Rod Johnson
 */
public class DriverManagerDataSourceTests {

	private Connection connection = mock();

	@Test
	public void testStandardUsage() throws Exception {
		final String jdbcUrl = "url";
		final String uname = "uname";
		final String pwd = "pwd";

		class TestDriverManagerDataSource extends DriverManagerDataSource {
			@Override
			protected Connection getConnectionFromDriverManager(String url, Properties props) {
				assertThat(url).isEqualTo(jdbcUrl);
				assertThat(props.getProperty("user")).isEqualTo(uname);
				assertThat(props.getProperty("password")).isEqualTo(pwd);
				return connection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setUsername(uname);
		ds.setPassword(pwd);

		Connection actualCon = ds.getConnection();
		assertThat(actualCon).isSameAs(connection);

		assertThat(ds.getUrl()).isEqualTo(jdbcUrl);
		assertThat(ds.getPassword()).isEqualTo(pwd);
		assertThat(ds.getUsername()).isEqualTo(uname);
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
				assertThat(url).isEqualTo(jdbcUrl);
				assertThat(props.getProperty("user")).isEqualTo("uname");
				assertThat(props.getProperty("password")).isEqualTo("pwd");
				assertThat(props.getProperty("myProp")).isEqualTo("myValue");
				assertThat(props.getProperty("yourProp")).isEqualTo("yourValue");
				return connection;
			}
		}

		DriverManagerDataSource ds = new TestDriverManagerDataSource();
		//ds.setDriverClassName("foobar");
		ds.setUrl(jdbcUrl);
		ds.setConnectionProperties(connProps);

		Connection actualCon = ds.getConnection();
		assertThat(actualCon).isSameAs(connection);

		assertThat(ds.getUrl()).isEqualTo(jdbcUrl);
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
				assertThat(url).isEqualTo(jdbcUrl);
				assertThat(props.getProperty("user")).isEqualTo(uname);
				assertThat(props.getProperty("password")).isEqualTo(pwd);
				assertThat(props.getProperty("myProp")).isEqualTo("myValue");
				assertThat(props.getProperty("yourProp")).isEqualTo("yourValue");
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
		assertThat(actualCon).isSameAs(connection);

		assertThat(ds.getUrl()).isEqualTo(jdbcUrl);
		assertThat(ds.getPassword()).isEqualTo(pwd);
		assertThat(ds.getUsername()).isEqualTo(uname);
	}

	@Test
	public void testInvalidClassName() throws Exception {
		String bogusClassName = "foobar";
		DriverManagerDataSource ds = new DriverManagerDataSource();
		assertThatIllegalStateException().isThrownBy(() ->
				ds.setDriverClassName(bogusClassName))
			.withCauseInstanceOf(ClassNotFoundException.class);
	}

}
