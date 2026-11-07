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
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_DEFAULT;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;

/**
 * Tests for {@link IsolationLevelDataSourceAdapter}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class IsolationLevelDataSourceAdapterTests {

	private final IsolationLevelDataSourceAdapter adapter = new IsolationLevelDataSourceAdapter();


	@Test
	void setIsolationLevelNameToUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName(null));
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName("bogus"));
	}

	/**
	 * Verify that the internal 'constants' map is properly configured for all
	 * ISOLATION_ constants defined in {@link TransactionDefinition}.
	 */
	@Test
	void setIsolationLevelNameToAllSupportedValues() {
		Set<Integer> uniqueValues = new HashSet<>();
		streamIsolationConstants()
				.forEach(name -> {
					adapter.setIsolationLevelName(name);
					Integer isolationLevel = adapter.getIsolationLevel();
					if ("ISOLATION_DEFAULT".equals(name)) {
						assertThat(isolationLevel).isNull();
						uniqueValues.add(ISOLATION_DEFAULT);
					}
					else {
						Integer expected = IsolationLevelDataSourceAdapter.constants.get(name);
						assertThat(isolationLevel).isEqualTo(expected);
						uniqueValues.add(isolationLevel);
					}
				});
		assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(IsolationLevelDataSourceAdapter.constants.values());
	}

	@Test
	void setIsolationLevel() {
		assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevel(999));

		adapter.setIsolationLevel(ISOLATION_DEFAULT);
		assertThat(adapter.getIsolationLevel()).isNull();

		adapter.setIsolationLevel(ISOLATION_READ_COMMITTED);
		assertThat(adapter.getIsolationLevel()).isEqualTo(ISOLATION_READ_COMMITTED);
	}

	@Test
	void connectionClosedWhenSettingIsolationLevelFails() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		when(dataSource.getConnection()).thenReturn(connection);
		SQLException ex = new SQLException("Isolation level not supported");
		doThrow(ex).when(connection).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

		adapter.setTargetDataSource(dataSource);
		adapter.setIsolationLevel(ISOLATION_REPEATABLE_READ);

		assertThatExceptionOfType(SQLException.class).isThrownBy(adapter::getConnection).isSameAs(ex);
		verify(connection).close();
	}


	private static Stream<String> streamIsolationConstants() {
		return Arrays.stream(TransactionDefinition.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName)
				.filter(name -> name.startsWith("ISOLATION_"));
	}

}
