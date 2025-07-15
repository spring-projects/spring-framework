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
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
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
				.isThrownBy(() -> HttpMessageConverters.forClient().stringMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("stringMessageConverter should support 'text/plain'");
	}

	@Test
	void failsWhenJsonConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().jsonMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("jsonMessageConverter should support 'application/json'");
	}

	@Test
	void failsWhenXmlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().xmlMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("xmlMessageConverter should support 'text/xml'");
	}

	@Test
	void failsWhenSmileConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().smileMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("smileMessageConverter should support 'application/x-jackson-smile'");
	}

	@Test
	void failsWhenCborConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().cborMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("cborMessageConverter should support 'application/cbor'");
	}

	@Test
	void failsWhenYamlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> HttpMessageConverters.forClient().yamlMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("yamlMessageConverter should support 'application/yaml'");
	}


	@Nested
	class ClientConvertersTests {

		@Test
		void defaultConverters() {
			var converters = HttpMessageConverters.forClient().registerDefaults().build();
			assertThat(converters).hasExactlyElementsOfTypes(ByteArrayHttpMessageConverter.class,
					StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}

		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.forClient().registerDefaults().build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

			assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class,
					JacksonSmileHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverter() {
			var converters = HttpMessageConverters.forClient()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverterAheadOfDefaults() {
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(
					CustomHttpMessageConverter.class, ByteArrayHttpMessageConverter.class,
					StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}

		@Test
		void registerMultipartConverterWhenOtherConvertersPresent() {
			var converters = HttpMessageConverters.forClient()
					.stringMessageConverter(new StringHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(StringHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void shouldUseSpecificConverter() {
			var jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.forClient().registerDefaults()
					.jsonMessageConverter(jacksonConverter).build();

			var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}

		@Test
		void shouldConfigureConverter() {
			var customConverter = new CustomHttpMessageConverter();
			HttpMessageConverters.forClient()
					.customMessageConverter(customConverter)
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
					AllEncompassingFormHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}

		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.forServer().registerDefaults().build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);

			assertThat(multipartConverter.getPartConverters()).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, JacksonJsonHttpMessageConverter.class,
					JacksonSmileHttpMessageConverter.class, JacksonCborHttpMessageConverter.class,
					JacksonYamlHttpMessageConverter.class, JacksonXmlHttpMessageConverter.class,
					KotlinSerializationProtobufHttpMessageConverter.class, AtomFeedHttpMessageConverter.class,
					RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverter() {
			var converters = HttpMessageConverters.forServer()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void registerCustomMessageConverterAheadOfDefaults() {
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(
					CustomHttpMessageConverter.class,
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.customMessageConverter(new CustomHttpMessageConverter()).build();
			var multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters);
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}

		@Test
		void registerMultipartConverterWhenOtherConvertersPresent() {
			var converters = HttpMessageConverters.forServer()
					.stringMessageConverter(new StringHttpMessageConverter()).build();
			assertThat(converters).hasExactlyElementsOfTypes(StringHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void shouldUseSpecificConverter() {
			var jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.forServer().registerDefaults()
					.jsonMessageConverter(jacksonConverter).build();

			var customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters);
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}

		@Test
		void shouldConfigureConverter() {
			var customConverter = new CustomHttpMessageConverter();
			HttpMessageConverters.forServer().registerDefaults()
					.customMessageConverter(customConverter)
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
