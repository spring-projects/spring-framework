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

package org.springframework.test.context.cache;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import org.springframework.core.SpringProperties;
import org.springframework.test.context.cache.ContextCache.PauseMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.context.cache.ContextCache.CONTEXT_CACHE_PAUSE_PROPERTY_NAME;
import static org.springframework.test.context.cache.ContextCache.DEFAULT_MAX_CONTEXT_CACHE_SIZE;
import static org.springframework.test.context.cache.ContextCache.MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME;
import static org.springframework.test.context.cache.ContextCacheUtils.retrieveMaxCacheSize;
import static org.springframework.test.context.cache.ContextCacheUtils.retrievePauseMode;

/**
 * Tests for {@link ContextCacheUtils}.
 *
 * @author Sam Brannen
 * @since 4.3
 */
class ContextCacheUtilsTests {

	@Nested
	class MaxCacheSizeTests {

		@BeforeEach
		@AfterEach
		void clearProperties() {
			System.clearProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME);
			SpringProperties.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, null);
		}

		@Test
		void retrieveMaxCacheSizeFromDefault() {
			assertDefaultValue();
		}

		@Test
		void retrieveMaxCacheSizeFromBogusSystemProperty() {
			System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "bogus");
			assertDefaultValue();
		}

		@Test
		void retrieveMaxCacheSizeFromBogusSpringProperty() {
			SpringProperties.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "bogus");
			assertDefaultValue();
		}

		@Test
		void retrieveMaxCacheSizeFromDecimalSpringProperty() {
			SpringProperties.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "3.14");
			assertDefaultValue();
		}

		@Test
		void retrieveMaxCacheSizeFromSystemProperty() {
			System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "42");
			assertThat(retrieveMaxCacheSize()).isEqualTo(42);
		}

		@Test
		void retrieveMaxCacheSizeFromSystemPropertyContainingWhitespace() {
			System.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "42\t");
			assertThat(retrieveMaxCacheSize()).isEqualTo(42);
		}

		@Test
		void retrieveMaxCacheSizeFromSpringProperty() {
			SpringProperties.setProperty(MAX_CONTEXT_CACHE_SIZE_PROPERTY_NAME, "99");
			assertThat(retrieveMaxCacheSize()).isEqualTo(99);
		}

		private static void assertDefaultValue() {
			assertThat(retrieveMaxCacheSize()).isEqualTo(DEFAULT_MAX_CONTEXT_CACHE_SIZE);
		}
	}

	/**
	 * Tests for {@link PauseMode} support.
	 * @since 7.0.3
	 */
	@Nested
	class PauseModeTests {

		static final String[] ALWAYS_VALUES = {
				"always",
				"Always",
				"ALWAYS",
				"\talways\u000B"
			};

		static final String[] NEVER_VALUES = {
				"never",
				"Never",
				"NEVER",
				"\tnever\u000B"
			};


		@BeforeEach
		@AfterEach
		void clearProperties() {
			System.clearProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME);
			SpringProperties.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, null);
		}

		@Test
		void retrievePauseModeFromDefault() {
			assertThat(retrievePauseMode()).isEqualTo(PauseMode.ALWAYS);
		}

		@Test
		void retrievePauseModeFromBogusSystemProperty() {
			System.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, "bogus");
			asssertUnsupportedValue();
		}

		@Test
		void retrievePauseModeFromBogusSpringProperty() {
			SpringProperties.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, "bogus");
			asssertUnsupportedValue();
		}

		@ParameterizedTest
		@FieldSource("ALWAYS_VALUES")
		void retrievePauseModeFromSystemPropertyWithValueAlways(String value) {
			System.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, value);
			assertThat(retrievePauseMode()).isEqualTo(PauseMode.ALWAYS);
		}

		@ParameterizedTest
		@FieldSource("NEVER_VALUES")
		void retrievePauseModeFromSystemPropertyWithValueNever(String value) {
			System.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, value);
			assertThat(retrievePauseMode()).isEqualTo(PauseMode.NEVER);
		}

		@ParameterizedTest
		@FieldSource("ALWAYS_VALUES")
		void retrievePauseModeFromSpringPropertyWithValueAlways(String value) {
			SpringProperties.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, value);
			assertThat(retrievePauseMode()).isEqualTo(PauseMode.ALWAYS);
		}

		@ParameterizedTest
		@FieldSource("NEVER_VALUES")
		void retrievePauseModeFromSpringPropertyWithValueNever(String value) {
			SpringProperties.setProperty(CONTEXT_CACHE_PAUSE_PROPERTY_NAME, value);
			assertThat(retrievePauseMode()).isEqualTo(PauseMode.NEVER);
		}

		private void asssertUnsupportedValue() {
			assertThatIllegalArgumentException()
					.isThrownBy(ContextCacheUtils::retrievePauseMode)
					.withMessage("Unsupported value 'bogus' for property 'spring.test.context.cache.pause'");
		}
	}

}
