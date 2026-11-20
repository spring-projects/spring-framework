/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

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


	private static Stream<String> streamIsolationConstants() {
		return Arrays.stream(TransactionDefinition.class.getFields())
				.filter(ReflectionUtils::isPublicStaticFinal)
				.map(Field::getName)
				.filter(name -> name.startsWith("ISOLATION_"));
	}

}
