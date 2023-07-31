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

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link LazyConnectionDataSourceProxy}.
 *
 * @author Sam Brannen
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


	private static Stream<String> streamIsolationConstants() {
		return Arrays.stream(Connection.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName)
				.filter(name -> name.startsWith("TRANSACTION_"));
	}

}
