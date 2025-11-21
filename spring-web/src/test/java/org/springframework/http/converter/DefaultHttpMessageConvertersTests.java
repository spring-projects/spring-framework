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

package org.springframework.http.converter;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
import org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.protobuf.KotlinSerializationProtobufHttpMessageConverter;
import org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultHttpMessageConverters}.
 */
@SuppressWarnings("removal")
class DefaultHttpMessageConvertersTests {

	@ParameterizedTest
	@MethodSource("emptyMessageConverters")
	void emptyConverters(Iterable<HttpMessageConverter<?>> converters) {
		assertThat(converters).isEmpty();
	}

	static Stream<Iterable<HttpMessageConverter<?>>> emptyMessageConverters() {
		return Stream.of(
				HttpMessageConverters.forClient().build(),
				HttpMessageConverters.forServer().build()
		);
	}

	@Test
	void failsWhenStringConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withStringConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'text/plain'");
	}

	@Test
	void failsWhenJsonConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withJsonConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'application/json'");
	}

	@Test
	void canConfigureXmlConverterWithCharset() {
		HttpMessageConverters.forClient().withXmlConverter(new JacksonXmlHttpMessageConverter()).build();
	}

	@Test
	void failsWhenXmlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withXmlConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'text/xml'");
	}

	@Test
	void failsWhenSmileConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withSmileConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'application/x-jackson-smile'");
	}

	@Test
	void failsWhenCborConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withCborConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'application/cbor'");
	}

	@Test
	void failsWhenYamlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().withYamlConverter(new CustomHttpMessageConverter()).build())
				.withMessage("converter should support 'application/yaml'");
	}


	@Nested
	class ClientConvertersTests {

		@Test
		void defaultConverters() {
			var converters = HttpMessageConverters.forClient().registerDefaults().build();
			assertThat(converters).hasExactlyElementsOfTypes(ByteArrayHttpMessageConverter.class,
					StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.forClient().registerDefaults().build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

			assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverter() {
			var converters = HttpMessageConverters.forClient()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverterAheadOfDefaults() {
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(
					CustomHttpMessageConverter.class, ByteArrayHttpMessageConverter.class,
					StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}

		@Test
		void shouldNotConfigureOverridesWhenDefaultOff() {
			var stringConverter = new StringHttpMessageConverter();
			var converters = HttpMessageConverters.forClient().withStringConverter(stringConverter).build();
			assertThat(converters).isEmpty();
		}

		@Test
		void shouldUseSpecificConverter() {
			var jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.withJsonConverter(jacksonConverter).build();

			var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}

		@Test
		void shouldOverrideStringConverters() {
			var stringConverter = new StringHttpMessageConverter();
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.withStringConverter(stringConverter).build();

			var actualConverter = findMessageConverter(StringHttpMessageConverter.class, converters);
			assertThat(actualConverter).isEqualTo(stringConverter);
		}

		@Test
		void shouldConfigureConverter() {
			var customConverter = new CustomHttpMessageConverter();
			HttpMessageConverters.forClient()
					.addCustomConverter(customConverter)
					.configureMessageConverters(converter -> {
						if (converter instanceof CustomHttpMessageConverter custom) {
							custom.processed = true;
						}
					}).build();

			assertThat(customConverter.processed).isTrue();
		}

	}


	@Nested
	class ServerConvertersTests {

		@Test
		void defaultConverters() {
			var converters = HttpMessageConverters.forServer().registerDefaults().build();
			assertThat(converters).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.forServer().registerDefaults().build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

			assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverter() {
			var converters = HttpMessageConverters.forServer()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverterAheadOfDefaults() {
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(
					CustomHttpMessageConverter.class,
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class, KotlinSerializationJsonHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					KotlinSerializationCborHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.addCustomConverter(new CustomHttpMessageConverter()).build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}


		@Test
		void shouldNotConfigureOverridesWhenDefaultOff() {
			var stringConverter = new StringHttpMessageConverter();
			var converters = HttpMessageConverters.forServer().withStringConverter(stringConverter).build();
			assertThat(converters).isEmpty();
		}

		@Test
		void shouldUseSpecificConverter() {
			var jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.withJsonConverter(jacksonConverter).build();

			var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}


		@Test
		void shouldOverrideStringConverters() {
			var stringConverter = new StringHttpMessageConverter();
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.withStringConverter(stringConverter).build();

			var actualConverter = findMessageConverter(StringHttpMessageConverter.class, converters);
			assertThat(actualConverter).isEqualTo(stringConverter);
		}

		@Test
		void shouldConfigureConverter() {
			var customConverter = new CustomHttpMessageConverter();
			HttpMessageConverters.forServer().registerDefaults()
					.addCustomConverter(customConverter)
					.configureMessageConverters(converter -> {
						if (converter instanceof CustomHttpMessageConverter custom) {
							custom.processed = true;
						}
					}).build();

			assertThat(customConverter.processed).isTrue();
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T findMessageConverter(Class<T> converterType, Iterable<HttpMessageConverter<?>> converters) {
		return (T) StreamSupport
				.stream(converters.spliterator(), false)
				.filter(converter -> converter.getClass().equals(converterType))
				.findFirst().orElseThrow();
	}

	static class CustomHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

		boolean processed = false;

		@Override
		protected boolean supports(Class<?> clazz) {
			return false;
		}

		@Override
		protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
			return null;
		}

		@Override
		protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {

		}
	}

}
