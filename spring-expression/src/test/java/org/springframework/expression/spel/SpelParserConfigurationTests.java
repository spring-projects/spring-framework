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

package org.springframework.expression.spel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SpelParserConfiguration}.
 *
 * @author Sam Brannen
 * @since 7.0.10
 */
class SpelParserConfigurationTests {

	@Test
	@SuppressWarnings("deprecation")
	void builderAppliesSameDefaultsAsNoArgConstructor() {
		SpelParserConfiguration expected = new SpelParserConfiguration();
		SpelParserConfiguration actual = SpelParserConfiguration.builder().build();

		assertThat(actual.getCompilerMode()).isEqualTo(expected.getCompilerMode());
		assertThat(actual.getCompilerClassLoader()).isEqualTo(expected.getCompilerClassLoader());
		assertThat(actual.isAutoGrowNullReferences()).isEqualTo(expected.isAutoGrowNullReferences());
		assertThat(actual.isAutoGrowCollections()).isEqualTo(expected.isAutoGrowCollections());
		assertThat(actual.getMaximumAutoGrowSize()).isEqualTo(expected.getMaximumAutoGrowSize());
		assertThat(actual.getMaximumExpressionLength()).isEqualTo(expected.getMaximumExpressionLength());
		assertThat(actual.getMaximumOperations()).isEqualTo(expected.getMaximumOperations());
		assertThat(actual.getMaximumBigPowerBits()).isEqualTo(expected.getMaximumBigPowerBits());
		assertThat(actual.getMaximumNestingDepth()).isEqualTo(expected.getMaximumNestingDepth());
	}

	@Test
	void withDefaultsMatchesBuilderDefaults() {
		SpelParserConfiguration expected = SpelParserConfiguration.builder().build();
		SpelParserConfiguration actual = SpelParserConfiguration.withDefaults();

		assertThat(actual.getCompilerMode()).isEqualTo(expected.getCompilerMode());
		assertThat(actual.getCompilerClassLoader()).isEqualTo(expected.getCompilerClassLoader());
		assertThat(actual.isAutoGrowNullReferences()).isEqualTo(expected.isAutoGrowNullReferences());
		assertThat(actual.isAutoGrowCollections()).isEqualTo(expected.isAutoGrowCollections());
		assertThat(actual.getMaximumAutoGrowSize()).isEqualTo(expected.getMaximumAutoGrowSize());
		assertThat(actual.getMaximumExpressionLength()).isEqualTo(expected.getMaximumExpressionLength());
		assertThat(actual.getMaximumOperations()).isEqualTo(expected.getMaximumOperations());
		assertThat(actual.getMaximumBigPowerBits()).isEqualTo(expected.getMaximumBigPowerBits());
		assertThat(actual.getMaximumNestingDepth()).isEqualTo(expected.getMaximumNestingDepth());
	}

	@Test
	void builderAppliesCustomValues() {
		ClassLoader classLoader = getClass().getClassLoader();

		SpelParserConfiguration configuration = SpelParserConfiguration.builder()
				.compilerMode(SpelCompilerMode.IMMEDIATE)
				.compilerClassLoader(classLoader)
				.autoGrowNullReferences()
				.autoGrowCollections()
				.maximumAutoGrowSize(99)
				.maximumExpressionLength(100)
				.maximumOperations(101)
				.maximumBigPowerBits(102)
				.maximumNestingDepth(103)
				.build();

		assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
		assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
		assertThat(configuration.isAutoGrowNullReferences()).isTrue();
		assertThat(configuration.isAutoGrowCollections()).isTrue();
		assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
		assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
		assertThat(configuration.getMaximumOperations()).isEqualTo(101);
		assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(102);
		assertThat(configuration.getMaximumNestingDepth()).isEqualTo(103);
	}

	@Test
	void builderRejectsInvalidValues() {
		SpelParserConfiguration.Builder builder = SpelParserConfiguration.builder();

		assertThatIllegalArgumentException().isThrownBy(() -> builder.compilerMode(null));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumAutoGrowSize(-1));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumExpressionLength(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumOperations(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumBigPowerBits(0));
		assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumNestingDepth(0));
	}


	/**
	 * Regression tests for the legacy constructors in {@link SpelParserConfiguration}.
	 * <p>Elsewhere in the test suite, we prefer {@link SpelParserConfiguration#builder()}
	 * over these constructors. Thus, this nested class exists so that constructor coverage
	 * remains in one place, which will also keep any future deprecation warnings for
	 * these constructors confined to this class.
	 */
	@Nested
	@SuppressWarnings("deprecation")
	class LegacyConstructorTests {

		@Test
		void noArgConstructorAppliesDefaults() {
			SpelParserConfiguration configuration = new SpelParserConfiguration();

			assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.OFF);
			assertThat(configuration.getCompilerClassLoader()).isNull();
			assertThat(configuration.isAutoGrowNullReferences()).isFalse();
			assertThat(configuration.isAutoGrowCollections()).isFalse();
			assertThat(configuration.getMaximumAutoGrowSize())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_AUTO_GROW_SIZE);
			assertThat(configuration.getMaximumExpressionLength())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH);
			assertThat(configuration.getMaximumOperations())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_OPERATIONS);
			assertThat(configuration.getMaximumBigPowerBits())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_BIG_POWER_BITS);
		}

		@Test
		void compilerModeAndClassLoaderConstructor() {
			ClassLoader classLoader = getClass().getClassLoader();
			SpelParserConfiguration configuration =
					new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, classLoader);

			assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
			assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
			assertThat(configuration.isAutoGrowNullReferences()).isFalse();
			assertThat(configuration.isAutoGrowCollections()).isFalse();
			assertThat(configuration.getMaximumAutoGrowSize())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_AUTO_GROW_SIZE);
		}

		@Test
		void autoGrowFlagsConstructor() {
			SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);

			assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.OFF);
			assertThat(configuration.isAutoGrowNullReferences()).isTrue();
			assertThat(configuration.isAutoGrowCollections()).isTrue();
			assertThat(configuration.getMaximumAutoGrowSize())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_AUTO_GROW_SIZE);
		}

		@Test
		void autoGrowFlagsAndMaximumAutoGrowSizeConstructor() {
			SpelParserConfiguration configuration = new SpelParserConfiguration(true, true, 99);

			assertThat(configuration.isAutoGrowNullReferences()).isTrue();
			assertThat(configuration.isAutoGrowCollections()).isTrue();
			assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
		}

		@Test
		void fiveArgConstructorAppliesAllValues() {
			ClassLoader classLoader = getClass().getClassLoader();
			SpelParserConfiguration configuration = new SpelParserConfiguration(
					SpelCompilerMode.IMMEDIATE, classLoader, true, true, 99);

			assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
			assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
			assertThat(configuration.isAutoGrowNullReferences()).isTrue();
			assertThat(configuration.isAutoGrowCollections()).isTrue();
			assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
			assertThat(configuration.getMaximumExpressionLength())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH);
		}

		@Test
		void sixArgConstructorAppliesMaximumExpressionLength() {
			SpelParserConfiguration configuration = new SpelParserConfiguration(
					SpelCompilerMode.IMMEDIATE, null, true, true, 99, 100);

			assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
			assertThat(configuration.getMaximumOperations())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_OPERATIONS);
		}

		@Test
		void sevenArgConstructorAppliesMaximumOperations() {
			SpelParserConfiguration configuration = new SpelParserConfiguration(
					SpelCompilerMode.IMMEDIATE, null, true, true, 99, 100, 101);

			assertThat(configuration.getMaximumOperations()).isEqualTo(101);
			assertThat(configuration.getMaximumBigPowerBits())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_BIG_POWER_BITS);
		}

		@Test
		void eightArgConstructorAppliesMaximumBigPowerBits() {
			SpelParserConfiguration configuration = new SpelParserConfiguration(
					SpelCompilerMode.IMMEDIATE, null, true, true, 99, 100, 101, 102);

			assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(102);
			assertThat(configuration.getMaximumNestingDepth())
					.isEqualTo(SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_NESTING_DEPTH);
		}

		@Test
		void canonicalConstructorAppliesAllValues() {
			ClassLoader classLoader = getClass().getClassLoader();
			SpelParserConfiguration configuration = new SpelParserConfiguration(
					SpelCompilerMode.IMMEDIATE, classLoader, true, true, 99, 100, 101, 102, 103);

			assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
			assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
			assertThat(configuration.isAutoGrowNullReferences()).isTrue();
			assertThat(configuration.isAutoGrowCollections()).isTrue();
			assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
			assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
			assertThat(configuration.getMaximumOperations()).isEqualTo(101);
			assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(102);
			assertThat(configuration.getMaximumNestingDepth()).isEqualTo(103);
		}

		@Test
		void canonicalConstructorRejectsInvalidValues() {
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(null, null, false, false, 0, 1, 1, 1, 1));
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, -1, 1, 1, 1, 1));
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 0, 1, 1, 1));
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 1, 0, 1, 1));
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 1, 1, 0, 1));
			assertThatIllegalArgumentException().isThrownBy(() ->
					new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 1, 1, 1, 0));
		}

	}

}
