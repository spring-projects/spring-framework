/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Field;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LazyConnectionDataSourceProxy}.
 *
 * @author Sam Brannen
 * @author Chengang Guan
 * @since 6.1
 */
class LazyConnectionDataSourceProxyTests {

	private final LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();


	@Test
	void setDefaultTransactionIsolationNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("bogus"));
	}

	/**
	 * Verify that the internal 'constants' map is properly configured for all
	 * TRANSACTION_ constants defined in {@link java.sql.Connection}.
	 */
	@Test
	void setDefaultTransactionIsolationNameToAllSupportedValues() {
		Set<Integer> uniqueValues = new HashSet<>();
		streamIsolationConstants()
				.forEach(name -> {
					if ("TRANSACTION_NONE".equals(name)) {
						assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(name));
					}
					else {
						proxy.setDefaultTransactionIsolationName(name);
						Integer defaultTransactionIsolation = proxy.defaultTransactionIsolation();
						Integer expected = LazyConnectionDataSourceProxy.constants.get(name);
						assertThat(defaultTransactionIsolation).isEqualTo(expected);
						uniqueValues.add(defaultTransactionIsolation);
					}
				});
		assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(LazyConnectionDataSourceProxy.constants.values());
	}

	@Test
	void setDefaultTransactionIsolation() {
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(-999));
		assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(TRANSACTION_NONE));

		proxy.setDefaultTransactionIsolation(TRANSACTION_READ_COMMITTED);
		assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);

		proxy.setDefaultTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
		assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_UNCOMMITTED);

		proxy.setDefaultTransactionIsolation(TRANSACTION_REPEATABLE_READ);
		assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_REPEATABLE_READ);

		proxy.setDefaultTransactionIsolation(TRANSACTION_SERIALIZABLE);
		assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
	}

	@Test
	void lazyHandingCatalog() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setCatalog("catalogName");
		assertThat(lazyConnection.getCatalog()).isEqualTo("catalogName");
		assertThat(physicalConnection.getCatalog()).isNull();
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getCatalog()).isEqualTo("catalogName");
		assertThat(physicalConnection.getCatalog()).isEqualTo("catalogName");
	}

	@Test
	void lazyHandingSchema() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setSchema("schemaName");
		assertThat(lazyConnection.getSchema()).isEqualTo("schemaName");
		assertThat(physicalConnection.getSchema()).isNull();
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getSchema()).isEqualTo("schemaName");
		assertThat(physicalConnection.getSchema()).isEqualTo("schemaName");
	}

	@Test
	void lazyHandingHoldability() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertThat(lazyConnection.getHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertThat(physicalConnection.getHoldability()).isEqualTo(0);
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
		assertThat(physicalConnection.getHoldability()).isEqualTo(ResultSet.CLOSE_CURSORS_AT_COMMIT);
	}

	@Test
	void lazyHandingTransactionIsolation() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setTransactionIsolation(TRANSACTION_SERIALIZABLE);
		assertThat(lazyConnection.getTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
		assertThat(physicalConnection.getTransactionIsolation()).isEqualTo(0);
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
		assertThat(physicalConnection.getTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
	}

	@Test
	void lazyHandingAutoCommit() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setAutoCommit(true);
		assertThat(lazyConnection.getAutoCommit()).isTrue();
		assertThat(physicalConnection.getAutoCommit()).isFalse();
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getAutoCommit()).isTrue();
		assertThat(physicalConnection.getAutoCommit()).isTrue();
	}

	@Test
	void lazyHandingNetworkTimeoutExecutor() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setNetworkTimeout(command -> {}, 1000);
		assertThat(lazyConnection.getNetworkTimeout()).isEqualTo(1000);
		assertThat(physicalConnection.getNetworkTimeout()).isEqualTo(0);
		establishPhysicalConnection(lazyConnection);
		assertThat(lazyConnection.getNetworkTimeout()).isEqualTo(1000);
		assertThat(physicalConnection.getNetworkTimeout()).isEqualTo(1000);
	}

	@Test
	void lazyHandingClientInfoForKV() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		lazyConnection.setClientInfo("k1", "v1");
		lazyConnection.setClientInfo("k2", "v2");
		assertThat(physicalConnection.getClientInfo("k1")).isNull();
		assertThat(physicalConnection.getClientInfo("k2")).isNull();
		assertThat(lazyConnection.getClientInfo("k1")).isEqualTo("v1"); // establishPhysicalConnection
		assertThat(lazyConnection.getClientInfo("k2")).isEqualTo("v2");
	}

	@Test
	void notLazyHandingClientInfoForProperties() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = new MockConnection();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		proxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = proxy.getConnection();
		Properties properties = new Properties();
		properties.setProperty("k1", "v1");
		properties.setProperty("k2", "v2");
		lazyConnection.setClientInfo(properties); // establishPhysicalConnection
		assertThat(physicalConnection.getClientInfo("k1")).isEqualTo("v1");
		assertThat(physicalConnection.getClientInfo("k2")).isEqualTo("v2");
		assertThat(lazyConnection.getClientInfo("k1")).isEqualTo("v1");
		assertThat(lazyConnection.getClientInfo("k2")).isEqualTo("v2");
	}


	private static Stream<String> streamIsolationConstants() {
		return Arrays.stream(Connection.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName)
				.filter(name -> name.startsWith("TRANSACTION_"));
	}

	private static void establishPhysicalConnection(Connection lazyConnection) throws SQLException {
		lazyConnection.prepareStatement("SELECT 1");
	}


	/**
	 * A rudimentary physical connection implementation for testing lazy-loading behavior.
	 */
	 static class MockConnection implements Connection {

		private String username;

		private String password;

		private String catalog;

		private String schema;

		private int holdability;

		private int transactionIsolation;

		private boolean autoCommit;

		private int networkTimeout;

		private Properties clientInfo;

		private boolean closed = false;

		public MockConnection() {
		}

		public MockConnection(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public Statement createStatement() throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql) throws SQLException {
			return null;
		}

		@Override
		public String nativeSQL(String sql) throws SQLException {
			return "";
		}

		@Override
		public void setAutoCommit(boolean autoCommit) throws SQLException {
			this.autoCommit = autoCommit;
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			return this.autoCommit;
		}

		@Override
		public void commit() throws SQLException {
		}

		@Override
		public void rollback() throws SQLException {
		}

		@Override
		public void close() throws SQLException {
			this.closed = true;
		}

		@Override
		public boolean isClosed() throws SQLException {
			return this.closed;
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			return null;
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			return false;
		}

		@Override
		public void setCatalog(String catalog) throws SQLException {
			this.catalog = catalog;
		}

		@Override
		public String getCatalog() throws SQLException {
			return this.catalog;
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			this.transactionIsolation = level;
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			return this.transactionIsolation;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			return null;
		}

		@Override
		public void clearWarnings() throws SQLException {
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
			return null;
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			return Map.of();
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
		}

		@Override
		public void setHoldability(int holdability) throws SQLException {
			this.holdability = holdability;
		}

		@Override
		public int getHoldability() throws SQLException {
			return this.holdability;
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			return null;
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			return null;
		}

		@Override
		public void rollback(Savepoint savepoint) throws SQLException {
		}

		@Override
		public void releaseSavepoint(Savepoint savepoint) throws SQLException {
		}

		@Override
		public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
			return null;
		}

		@Override
		public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
			return null;
		}

		@Override
		public Clob createClob() throws SQLException {
			return null;
		}

		@Override
		public Blob createBlob() throws SQLException {
			return null;
		}

		@Override
		public NClob createNClob() throws SQLException {
			return null;
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			return null;
		}

		@Override
		public boolean isValid(int timeout) throws SQLException {
			return false;
		}

		@Override
		public void setClientInfo(String name, String value) throws SQLClientInfoException {
			if (this.clientInfo == null) {
				this.clientInfo = new Properties();
			}
			this.clientInfo.put(name, value);
		}

		@Override
		public void setClientInfo(Properties properties) throws SQLClientInfoException {
			if (properties != null) {
				this.clientInfo = properties;
			}
		}

		@Override
		public String getClientInfo(String name) throws SQLException {
			if (this.clientInfo != null) {
				return this.clientInfo.getProperty(name);
			}
			return null;
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			if (this.clientInfo == null) {
				this.clientInfo = new Properties();
			}
			return this.clientInfo;
		}

		@Override
		public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
			return null;
		}

		@Override
		public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
			return null;
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			this.schema = schema;
		}

		@Override
		public String getSchema() throws SQLException {
			return this.schema;
		}

		@Override
		public void abort(Executor executor) throws SQLException {
		}

		@Override
		public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
			if (executor == null) {
				throw new SQLClientInfoException();
			}
			this.networkTimeout = milliseconds;
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			return this.networkTimeout;
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException {
			return false;
		}

	}

}
