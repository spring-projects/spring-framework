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

package org.springframework.jdbc.datasource.lookup;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_DEFAULT;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;

/**
 * Tests for {@link IsolationLevelDataSourceRouter}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class IsolationLevelDataSourceRouterTests {

	private final IsolationLevelDataSourceRouter router = new IsolationLevelDataSourceRouter();


	@Test
	void resolveSpecifiedLookupKeyForInvalidTypes() {
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(new Object()));
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey('X'));
	}

	@Test
	void resolveSpecifiedLookupKeyByNameForUnsupportedValues() {
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(null));
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey("   "));
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey("bogus"));
	}

	/**
	 * Verify that the internal 'constants' map is properly configured for all
	 * ISOLATION_ constants defined in {@link TransactionDefinition}.
	 */
	@Test
	void resolveSpecifiedLookupKeyByNameForAllSupportedValues() {
		Set<Integer> uniqueValues = new HashSet<>();
		streamIsolationConstants()
				.forEach(name -> {
					Integer isolationLevel = (Integer) router.resolveSpecifiedLookupKey(name);
					Integer expected = IsolationLevelDataSourceRouter.constants.get(name);
					assertThat(isolationLevel).isEqualTo(expected);
					uniqueValues.add(isolationLevel);
				});
		assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(IsolationLevelDataSourceRouter.constants.values());
	}

	@Test
	void resolveSpecifiedLookupKeyByInteger() {
		assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(999));

		assertThat(router.resolveSpecifiedLookupKey(ISOLATION_DEFAULT)).isEqualTo(ISOLATION_DEFAULT);
		assertThat(router.resolveSpecifiedLookupKey(ISOLATION_READ_UNCOMMITTED)).isEqualTo(ISOLATION_READ_UNCOMMITTED);
		assertThat(router.resolveSpecifiedLookupKey(ISOLATION_READ_COMMITTED)).isEqualTo(ISOLATION_READ_COMMITTED);
		assertThat(router.resolveSpecifiedLookupKey(ISOLATION_REPEATABLE_READ)).isEqualTo(ISOLATION_REPEATABLE_READ);
		assertThat(router.resolveSpecifiedLookupKey(ISOLATION_SERIALIZABLE)).isEqualTo(ISOLATION_SERIALIZABLE);
	}


	private static Stream<String> streamIsolationConstants() {
		return Arrays.stream(TransactionDefinition.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName)
				.filter(name -> name.startsWith("ISOLATION_"));
	}

}
