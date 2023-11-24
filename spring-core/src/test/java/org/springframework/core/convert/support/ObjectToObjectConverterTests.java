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

package org.springframework.core.convert.support;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConverterNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ObjectToObjectConverter}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.3.21
 * @see org.springframework.core.convert.converter.DefaultConversionServiceTests#convertObjectToObjectUsingValueOfMethod()
 */
class ObjectToObjectConverterTests {

	private final GenericConversionService conversionService = new GenericConversionService();


	@BeforeEach
	void setup() {
		conversionService.addConverter(new ObjectToObjectConverter());
	}


	/**
	 * This test effectively verifies that the {@link ObjectToObjectConverter}
	 * was properly registered with the {@link GenericConversionService}.
	 */
	@Test
	void nonStaticToTargetTypeSimpleNameMethodWithMatchingReturnType() {
		assertThat(conversionService.canConvert(Source.class, Data.class))
				.as("can convert Source to Data").isTrue();
		Data data = conversionService.convert(new Source("test"), Data.class);
		assertThat(data).asString().isEqualTo("test");
	}

	@Test
	void nonStaticToTargetTypeSimpleNameMethodWithDifferentReturnType() {
		assertThat(conversionService.canConvert(Text.class, Data.class))
				.as("can convert Text to Data").isFalse();
		assertThat(conversionService.canConvert(Text.class, Optional.class))
				.as("can convert Text to Optional").isFalse();
		assertThatExceptionOfType(ConverterNotFoundException.class)
				.as("convert Text to Data")
				.isThrownBy(() -> conversionService.convert(new Text("test"), Data.class));
	}

	@Test
	void staticValueOfFactoryMethodWithDifferentReturnType() {
		assertThat(conversionService.canConvert(String.class, Data.class))
				.as("can convert String to Data").isFalse();
		assertThatExceptionOfType(ConverterNotFoundException.class)
				.as("convert String to Data")
				.isThrownBy(() -> conversionService.convert("test", Data.class));
	}


	static class Source {

		private final String value;

		private Source(String value) {
			this.value = value;
		}

		public Data toData() {
			return new Data(this.value);
		}
	}


	static class Text {

		private final String value;

		private Text(String value) {
			this.value = value;
		}

		public Optional<Data> toData() {
			return Optional.of(new Data(this.value));
		}
	}


	static class Data {

		private final String value;

		private Data(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public static Optional<Data> valueOf(String string) {
			return (string != null ? Optional.of(new Data(string)) : Optional.empty());
		}
	}

}
