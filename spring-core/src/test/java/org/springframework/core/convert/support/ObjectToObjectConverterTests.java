/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConverterNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ObjectToObjectConverter}.
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
