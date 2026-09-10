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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

	private final LazyConnectionDataSourceProxy lazyProxy = new LazyConnectionDataSourceProxy();


	@BeforeEach
	void setup() {
		lazyProxy.setDefaultAutoCommit(false);
		lazyProxy.setDefaultTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
	}

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
	void lazyHandlingCatalog() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection1 = mock();
		Connection physicalConnection2 = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection1).thenReturn(physicalConnection2);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection1 = lazyProxy.getConnection();
		lazyConnection1.setCatalog("catalogName");
		assertThat(lazyConnection1.getCatalog()).isEqualTo("catalogName");
		verify(physicalConnection1,never()).setCatalog("catalogName");
		verify(physicalConnection1,never()).getCatalog();
		establishPhysicalConnection(lazyConnection1);
		verify(physicalConnection1).setCatalog("catalogName");

		Connection lazyConnection2 = lazyProxy.getConnection();
		lazyConnection2.getCatalog(); // establish physical connection immediately
		verify(physicalConnection2).getCatalog();
	}

	@Test
	void lazyHandlingSchema() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection1 = mock();
		Connection physicalConnection2 = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection1).thenReturn(physicalConnection2);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection1 = lazyProxy.getConnection();
		lazyConnection1.setSchema("schemaName");
		assertThat(lazyConnection1.getSchema()).isEqualTo("schemaName");
		verify(physicalConnection1,never()).setSchema("schemaName");
		verify(physicalConnection1,never()).getSchema();
		establishPhysicalConnection(lazyConnection1);
		verify(physicalConnection1).setSchema("schemaName");

		Connection lazyConnection2 = lazyProxy.getConnection();
		lazyConnection2.getSchema(); // establish physical connection immediately
		verify(physicalConnection2).getSchema();
	}

	@Test
	void lazyHandlingHoldability() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection1 = mock();
		Connection physicalConnection2 = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection1).thenReturn(physicalConnection2);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection1 = lazyProxy.getConnection();
		lazyConnection1.setHoldability(CLOSE_CURSORS_AT_COMMIT);
		assertThat(lazyConnection1.getHoldability()).isEqualTo(CLOSE_CURSORS_AT_COMMIT);
		verify(physicalConnection1,never()).setHoldability(CLOSE_CURSORS_AT_COMMIT);
		verify(physicalConnection1,never()).getHoldability();
		establishPhysicalConnection(lazyConnection1);
		verify(physicalConnection1).setHoldability(CLOSE_CURSORS_AT_COMMIT);

		Connection lazyConnection2 = lazyProxy.getConnection();
		lazyConnection2.getHoldability(); // establish physical connection immediately
		verify(physicalConnection2).getHoldability();
	}

	@Test
	void lazyHandlingTransactionIsolation() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = lazyProxy.getConnection();
		lazyConnection.setTransactionIsolation(TRANSACTION_READ_COMMITTED);
		assertThat(lazyConnection.getTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);
		verify(physicalConnection,never()).setTransactionIsolation(TRANSACTION_READ_COMMITTED);
		verify(physicalConnection,never()).getTransactionIsolation();
		establishPhysicalConnection(lazyConnection);
		verify(physicalConnection).setTransactionIsolation(TRANSACTION_READ_COMMITTED);
	}

	@Test
	void lazyHandlingAutoCommit() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = lazyProxy.getConnection();
		lazyConnection.setAutoCommit(true);
		assertThat(lazyConnection.getAutoCommit()).isTrue();
		verify(physicalConnection,never()).setAutoCommit(true);
		verify(physicalConnection,never()).getAutoCommit();
		establishPhysicalConnection(lazyConnection);
		verify(physicalConnection).setAutoCommit(true);
	}

	@Test
	void lazyHandlingNetworkTimeoutExecutor() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection1 = mock();
		Connection physicalConnection2 = mock();
		Executor executor = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection1).thenReturn(physicalConnection2);
		doThrow(SQLException.class).when(physicalConnection2).setNetworkTimeout(eq(null), anyInt());
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection1 = lazyProxy.getConnection();
		lazyConnection1.setNetworkTimeout(executor, 1000);
		assertThat(lazyConnection1.getNetworkTimeout()).isEqualTo(1000);
		verify(physicalConnection1,never()).setNetworkTimeout(executor, 1000);
		verify(physicalConnection1,never()).getNetworkTimeout();
		establishPhysicalConnection(lazyConnection1);
		verify(physicalConnection1).setNetworkTimeout(executor, 1000);

		// null executor
		Connection lazyConnection2 = lazyProxy.getConnection();
		lazyConnection2.setNetworkTimeout(null, 1000);
		assertThatThrownBy(() -> establishPhysicalConnection(lazyConnection2)).isInstanceOf(SQLException.class);
	}

	@Test
	void lazyHandlingClientInfoForKV() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = lazyProxy.getConnection();
		lazyConnection.setClientInfo("k1", "v1");
		lazyConnection.setClientInfo("k2", "v2");
		verify(physicalConnection, never()).setClientInfo("k1", "v1");
		verify(physicalConnection, never()).setClientInfo("k2", "v2");
		lazyConnection.getClientInfo("k1"); // establish physical connection immediately
		verify(physicalConnection).setClientInfo("k1", "v1");
		verify(physicalConnection).getClientInfo("k1");
	}

	@Test
	void nonLazyHandlingClientInfoForProperties() throws SQLException {
		DataSource mockDataSource = mock();
		Connection physicalConnection = mock();
		when(mockDataSource.getConnection()).thenReturn(physicalConnection);
		lazyProxy.setTargetDataSource(mockDataSource);

		Connection lazyConnection = lazyProxy.getConnection();
		Properties properties = new Properties();
		properties.setProperty("k1", "v1");
		properties.setProperty("k2", "v2");
		lazyConnection.setClientInfo(properties); // establish physical connection immediately
		verify(physicalConnection).setClientInfo(properties);
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

}
