/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;

import javax.sql.DataSource;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link DelegatingDataSource}.
 *
 * @author Phillip Webb
 */
public class DelegatingDataSourceTest {

	private DataSource delegate;

	private DelegatingDataSource dataSource;

	@Before
	public void setup() {
		this.delegate = EasyMock.createMock(DataSource.class);
		this.dataSource = new DelegatingDataSource(delegate);
	}

	@Test
	public void shouldDelegateGetConnection() throws Exception {
		Connection connection = EasyMock.createMock(Connection.class);
		EasyMock.expect(delegate.getConnection()).andReturn(connection);
		EasyMock.replay(delegate);
		assertThat(dataSource.getConnection(), is(connection));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateGetConnectionWithUsernameAndPassword() throws Exception {
		Connection connection = EasyMock.createMock(Connection.class);
		String username = "username";
		String password = "password";
		EasyMock.expect(delegate.getConnection(username, password)).andReturn(connection);
		EasyMock.replay(delegate);
		assertThat(dataSource.getConnection(username, password), is(connection));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateGetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		EasyMock.expect(delegate.getLogWriter()).andReturn(writer);
		EasyMock.replay(delegate);
		assertThat(dataSource.getLogWriter(), is(writer));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateSetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		delegate.setLogWriter(writer);
		EasyMock.expectLastCall();
		EasyMock.replay(delegate);
		dataSource.setLogWriter(writer);
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateGetLoginTimeout() throws Exception {
		int timeout = 123;
		EasyMock.expect(delegate.getLoginTimeout()).andReturn(timeout);
		EasyMock.replay(delegate);
		assertThat(dataSource.getLoginTimeout(), is(timeout));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateSetLoginTimeoutWithSeconds() throws Exception {
		int timeout = 123;
		delegate.setLoginTimeout(timeout);
		EasyMock.expectLastCall();
		EasyMock.replay(delegate);
		dataSource.setLoginTimeout(timeout);
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateUnwrapWithoutImplementing() throws Exception {
		ExampleWrapper wrapper = EasyMock.createMock(ExampleWrapper.class);
		EasyMock.expect(delegate.unwrap(ExampleWrapper.class)).andReturn(wrapper);
		EasyMock.replay(delegate);
		assertThat(dataSource.unwrap(ExampleWrapper.class), is(wrapper));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateUnwrapImplementing() throws Exception {
		dataSource = new DelegatingDataSourceWithWrapper();
		EasyMock.replay(delegate);
		assertThat(dataSource.unwrap(ExampleWrapper.class),
				is((ExampleWrapper) dataSource));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateIsWrapperForWithoutImplementing() throws Exception {
		EasyMock.expect(delegate.isWrapperFor(ExampleWrapper.class)).andReturn(true);
		EasyMock.replay(delegate);
		assertThat(dataSource.isWrapperFor(ExampleWrapper.class), is(true));
		EasyMock.verify(delegate);
	}

	@Test
	public void shouldDelegateIsWrapperForImplementing() throws Exception {
		dataSource = new DelegatingDataSourceWithWrapper();
		EasyMock.replay(delegate);
		assertThat(dataSource.isWrapperFor(ExampleWrapper.class), is(true));
		EasyMock.verify(delegate);
	}

	public static interface ExampleWrapper {
	}

	private static class DelegatingDataSourceWithWrapper extends DelegatingDataSource
			implements ExampleWrapper {
	}
}
