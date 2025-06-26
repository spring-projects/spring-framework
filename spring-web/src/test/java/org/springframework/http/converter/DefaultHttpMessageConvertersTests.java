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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.Gson;
import com.rometools.rome.feed.WireFeed;
import jakarta.json.bind.Jsonb;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.dataformat.smile.SmileMapper;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.springframework.core.SmartClassLoader;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.cbor.JacksonCborHttpMessageConverter;
import org.springframework.http.converter.cbor.KotlinSerializationCborHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.protobuf.KotlinSerializationProtobufHttpMessageConverter;
import org.springframework.http.converter.smile.JacksonSmileHttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;

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

	static Stream<Arguments> emptyMessageConverters() {
		return Stream.of(
				Arguments.of(HttpMessageConverters.create().build().forClient()),
				Arguments.of(HttpMessageConverters.create().build().forServer())
		);
	}

	@Test
	void clientAndServerConvertersAreShared() {
		var converters = HttpMessageConverters.withDefaults().build();
		Set<HttpMessageConverter<?>> allConverters = new HashSet<>();
		converters.forClient().forEach(allConverters::add);
		converters.forServer().forEach(allConverters::add);
		assertThat(allConverters).hasSize(15);
	}

	@Test
	void failsWhenStringConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpMessageConverters.create().stringMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("stringMessageConverter should support 'text/plain'");
	}

	@Test
	void failsWhenJsonConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				HttpMessageConverters.create().jsonMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("jsonMessageConverter should support 'application/json'");
	}

	@Test
	void failsWhenXmlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						HttpMessageConverters.create().xmlMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("xmlMessageConverter should support 'text/xml'");
	}

	@Test
	void failsWhenSmileConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						HttpMessageConverters.create().smileMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("smileMessageConverter should support 'application/x-jackson-smile'");
	}

	@Test
	void failsWhenCborConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						HttpMessageConverters.create().cborMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("cborMessageConverter should support 'application/cbor'");
	}

	@Test
	void failsWhenYamlConverterDoesNotSupportMediaType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
						HttpMessageConverters.create().yamlMessageConverter(new CustomHttpMessageConverter()).build())
				.withMessage("yamlMessageConverter should support 'application/yaml'");
	}

	@Nested
	class ClientConvertersTests {

		@Test
		void defaultConverters() {
			var converters = HttpMessageConverters.withDefaults().build();
			assertThat(converters.forClient()).hasExactlyElementsOfTypes(ByteArrayHttpMessageConverter.class,
					StringHttpMessageConverter.class, ResourceHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}


		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.withDefaults().build();
			AllEncompassingFormHttpMessageConverter multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters.forClient());

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
			var converters = HttpMessageConverters.create()
					.additionalMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters.forClient()).hasExactlyElementsOfTypes(AllEncompassingFormHttpMessageConverter.class, CustomHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.withDefaults()
					.additionalMessageConverter(new CustomHttpMessageConverter()).build();
			AllEncompassingFormHttpMessageConverter multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters.forClient());
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}

		@Test
		void shouldUseServerSpecificConverter() {
			JacksonJsonHttpMessageConverter jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.withDefaults()
					.configureClient(configurer -> configurer.jsonMessageConverter(jacksonConverter)).build();

			JacksonJsonHttpMessageConverter customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters.forClient());
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}

		@Test
		void shouldConfigureConverter() {
			CustomHttpMessageConverter customConverter = new CustomHttpMessageConverter();
			var converters = HttpMessageConverters.withDefaults()
					.additionalMessageConverter(customConverter)
					.configureClient(configurer -> {
						configurer.configureClientMessageConverters(converter -> {
							if (converter instanceof CustomHttpMessageConverter custom) {
								custom.processed = true;
							}
						});
					}).build();

			assertThat(customConverter.processed).isTrue();
		}

	}

	@Nested
	class ServerConvertersTests {

		@Test
		void defaultConverters() {
			var converters = HttpMessageConverters.withDefaults().build();
			assertThat(converters.forServer()).hasExactlyElementsOfTypes(
					ByteArrayHttpMessageConverter.class, StringHttpMessageConverter.class,
					ResourceHttpMessageConverter.class, ResourceRegionHttpMessageConverter.class,
					JacksonJsonHttpMessageConverter.class, JacksonSmileHttpMessageConverter.class,
					JacksonCborHttpMessageConverter.class, JacksonYamlHttpMessageConverter.class,
					JacksonXmlHttpMessageConverter.class, KotlinSerializationProtobufHttpMessageConverter.class,
					AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class,
					AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void multipartConverterContainsOtherConverters() {
			var converters = HttpMessageConverters.withDefaults().build();
			AllEncompassingFormHttpMessageConverter multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters.forServer());

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
			var converters = HttpMessageConverters.create()
					.additionalMessageConverter(new CustomHttpMessageConverter()).build();
			assertThat(converters.forServer()).hasExactlyElementsOfTypes(CustomHttpMessageConverter.class, AllEncompassingFormHttpMessageConverter.class);
		}

		@Test
		void registerCustomConverterInMultipartConverter() {
			var converters = HttpMessageConverters.withDefaults()
					.additionalMessageConverter(new CustomHttpMessageConverter()).build();
			AllEncompassingFormHttpMessageConverter multipartConverter = findMessageConverter(AllEncompassingFormHttpMessageConverter.class, converters.forServer());
			assertThat(multipartConverter.getPartConverters()).hasAtLeastOneElementOfType(CustomHttpMessageConverter.class);
		}

		@Test
		void shouldUseServerSpecificConverter() {
			JacksonJsonHttpMessageConverter jacksonConverter = new JacksonJsonHttpMessageConverter();
			var converters = HttpMessageConverters.withDefaults()
					.configureServer(configurer -> configurer.jsonMessageConverter(jacksonConverter)).build();

			JacksonJsonHttpMessageConverter customConverter = findMessageConverter(JacksonJsonHttpMessageConverter.class, converters.forServer());
			assertThat(customConverter).isEqualTo(jacksonConverter);
		}

		@Test
		void shouldConfigureConverter() {
			CustomHttpMessageConverter customConverter = new CustomHttpMessageConverter();
			var converters = HttpMessageConverters.withDefaults()
					.additionalMessageConverter(customConverter)
					.configureServer(configurer -> {
						configurer.configureServerMessageConverters(converter -> {
							if (converter instanceof CustomHttpMessageConverter custom) {
								custom.processed = true;
							}
						});
					}).build();

			assertThat(customConverter.processed).isTrue();
		}
	}

	@Nested
	class ClasspathDetectionTests {

		@Test
		void jsonUsesJackson2WhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(ObjectMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(MappingJackson2HttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonJsonHttpMessageConverter.class);
		}

		@Test
		void jsonUsesGsonWhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(GsonHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonJsonHttpMessageConverter.class, MappingJackson2HttpMessageConverter.class);
		}

		@Test
		void jsonUsesJsonbWhenJacksonAndGsonNotPresent() {
			var classLoader = new FilteredClassLoader(ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper.class, Gson.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(JsonbHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonJsonHttpMessageConverter.class, MappingJackson2HttpMessageConverter.class,
							GsonHttpMessageConverter.class);
		}

		@Test
		void jsonUsesKotlinWhenOthersNotPresent() {
			var classLoader = new FilteredClassLoader(ObjectMapper.class, com.fasterxml.jackson.databind.ObjectMapper.class, Gson.class, Jsonb.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(KotlinSerializationJsonHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonJsonHttpMessageConverter.class, MappingJackson2HttpMessageConverter.class,
							GsonHttpMessageConverter.class, JsonbHttpMessageConverter.class);
		}

		@Test
		void xmlUsesJackson2WhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(XmlMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(MappingJackson2XmlHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonXmlHttpMessageConverter.class);
		}

		@Test
		void xmlUsesJaxbWhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(XmlMapper.class, com.fasterxml.jackson.dataformat.xml.XmlMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(Jaxb2RootElementHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonXmlHttpMessageConverter.class, MappingJackson2XmlHttpMessageConverter.class);
		}

		@Test
		void smileUsesJackson2WhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(SmileMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(MappingJackson2SmileHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonSmileHttpMessageConverter.class);
		}

		@Test
		void cborUsesJackson2WhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(CBORMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(MappingJackson2CborHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonCborHttpMessageConverter.class);
		}

		@Test
		void cborUsesKotlinWhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(CBORMapper.class, com.fasterxml.jackson.dataformat.cbor.CBORFactory.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(KotlinSerializationCborHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonCborHttpMessageConverter.class, MappingJackson2CborHttpMessageConverter.class);
		}

		@Test
		void yamlUsesJackson2WhenJacksonNotPresent() {
			var classLoader = new FilteredClassLoader(YAMLMapper.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).hasAtLeastOneElementOfType(MappingJackson2YamlHttpMessageConverter.class)
					.doesNotHaveAnyElementsOfTypes(JacksonYamlHttpMessageConverter.class);
		}

		@Test
		void atomAndRssNotConfiguredWhenRomeNotPresent() {
			var classLoader = new FilteredClassLoader(WireFeed.class);
			var converters = new DefaultHttpMessageConverters.DefaultBuilder(true, classLoader).build();
			assertThat(converters.forServer()).doesNotHaveAnyElementsOfTypes(AtomFeedHttpMessageConverter.class, RssChannelHttpMessageConverter.class);
		}

	}


	@SuppressWarnings("unchecked")
	private <T> T findMessageConverter(Class<T> converterType, Iterable<HttpMessageConverter<?>> converters) {
		return (T) StreamSupport
				.stream(converters.spliterator(), false)
				.filter(converter -> converter.getClass().equals(converterType))
				.findFirst().orElseThrow();
	}


	static class FilteredClassLoader extends URLClassLoader implements SmartClassLoader {

		private final Collection<Class<?>> hiddenClasses;

		public FilteredClassLoader(Class<?>... hiddenClasses) {
			this(java.util.Arrays.asList(hiddenClasses));
		}

		FilteredClassLoader(Collection<Class<?>> hiddenClasses) {
			super(new URL[0], FilteredClassLoader.class.getClassLoader());
			this.hiddenClasses = hiddenClasses;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			for (Class<?> hiddenClass : this.hiddenClasses) {
				if (hiddenClass.getName().equals(name)) {
					throw new ClassNotFoundException();
				}
			}
			return super.loadClass(name, resolve);
		}

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
