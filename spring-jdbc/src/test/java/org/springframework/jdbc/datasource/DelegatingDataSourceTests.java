/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DelegatingDataSource}.
 *
 * @author Phillip Webb
 */
class DelegatingDataSourceTests {

	private final DataSource delegate = mock();

	private DelegatingDataSource dataSource = new DelegatingDataSource(delegate);


	@Test
	void shouldDelegateGetConnection() throws Exception {
		Connection connection = mock();
		given(delegate.getConnection()).willReturn(connection);
		assertThat(dataSource.getConnection()).isEqualTo(connection);
	}

	@Test
	void shouldDelegateGetConnectionWithUsernameAndPassword() throws Exception {
		Connection connection = mock();
		String username = "username";
		String password = "password";
		given(delegate.getConnection(username, password)).willReturn(connection);
		assertThat(dataSource.getConnection(username, password)).isEqualTo(connection);
	}

	@Test
	void shouldDelegateGetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		given(delegate.getLogWriter()).willReturn(writer);
		assertThat(dataSource.getLogWriter()).isEqualTo(writer);
	}

	@Test
	void shouldDelegateSetLogWriter() throws Exception {
		PrintWriter writer = new PrintWriter(new ByteArrayOutputStream());
		dataSource.setLogWriter(writer);
		verify(delegate).setLogWriter(writer);
	}

	@Test
	void shouldDelegateGetLoginTimeout() throws Exception {
		int timeout = 123;
		given(delegate.getLoginTimeout()).willReturn(timeout);
		assertThat(dataSource.getLoginTimeout()).isEqualTo(timeout);
	}

	@Test
	void shouldDelegateSetLoginTimeoutWithSeconds() throws Exception {
		int timeout = 123;
		dataSource.setLoginTimeout(timeout);
		verify(delegate).setLoginTimeout(timeout);
	}

	@Test
	void shouldDelegateUnwrapWithoutImplementing() throws Exception {
		ExampleWrapper wrapper = mock();
		given(delegate.unwrap(ExampleWrapper.class)).willReturn(wrapper);
		assertThat(dataSource.unwrap(ExampleWrapper.class)).isEqualTo(wrapper);
	}

	@Test
	void shouldDelegateUnwrapImplementing() throws Exception {
		dataSource = new DelegatingDataSourceWithWrapper();
		assertThat(dataSource.unwrap(ExampleWrapper.class)).isSameAs(dataSource);
	}

	@Test
	void shouldDelegateIsWrapperForWithoutImplementing() throws Exception {
		given(delegate.isWrapperFor(ExampleWrapper.class)).willReturn(true);
		assertThat(dataSource.isWrapperFor(ExampleWrapper.class)).isTrue();
	}

	@Test
	void shouldDelegateIsWrapperForImplementing() throws Exception {
		dataSource = new DelegatingDataSourceWithWrapper();
		assertThat(dataSource.isWrapperFor(ExampleWrapper.class)).isTrue();
	}


	public interface ExampleWrapper {
	}

	private static class DelegatingDataSourceWithWrapper extends DelegatingDataSource
			implements ExampleWrapper {
	}

}
