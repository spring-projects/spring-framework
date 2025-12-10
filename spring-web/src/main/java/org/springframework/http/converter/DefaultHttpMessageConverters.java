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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.http.MediaType;
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
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation for {@link HttpMessageConverters}.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 */
@SuppressWarnings("removal")
class DefaultHttpMessageConverters implements HttpMessageConverters {

	private final List<HttpMessageConverter<?>> messageConverters;


	DefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}


	@Override
	public boolean isEmpty() {
		return this.messageConverters.isEmpty();
	}

	@Override
	public Iterator<HttpMessageConverter<?>> iterator() {
		return this.messageConverters.iterator();
	}


	abstract static class DefaultBuilder {

		private static final boolean JACKSON_PRESENT;

		private static final boolean JACKSON_2_PRESENT;

		private static final boolean GSON_PRESENT;

		private static final boolean JSONB_PRESENT;

		private static final boolean KOTLIN_SERIALIZATION_JSON_PRESENT;

		private static final boolean JACKSON_XML_PRESENT;

		private static final boolean JACKSON_2_XML_PRESENT;

		private static final boolean JAXB_2_PRESENT;

		private static final boolean JACKSON_SMILE_PRESENT;

		private static final boolean JACKSON_2_SMILE_PRESENT;

		private static final boolean JACKSON_CBOR_PRESENT;

		private static final boolean JACKSON_2_CBOR_PRESENT;

		private static final boolean KOTLIN_SERIALIZATION_CBOR_PRESENT;

		private static final boolean JACKSON_YAML_PRESENT;

		private static final boolean JACKSON_2_YAML_PRESENT;

		private static final boolean KOTLIN_SERIALIZATION_PROTOBUF_PRESENT;

		private static final boolean ROME_PRESENT;

		boolean registerDefaults;

		@Nullable ByteArrayHttpMessageConverter byteArrayConverter;

		@Nullable HttpMessageConverter<?> stringConverter;

		@Nullable HttpMessageConverter<?> resourceConverter;

		@Nullable HttpMessageConverter<?> resourceRegionConverter;

		@Nullable Consumer<HttpMessageConverter<?>> configurer;

		@Nullable HttpMessageConverter<?> kotlinJsonConverter;

		@Nullable HttpMessageConverter<?> jsonConverter;

		@Nullable HttpMessageConverter<?> xmlConverter;

		@Nullable HttpMessageConverter<?> smileConverter;

		@Nullable HttpMessageConverter<?> kotlinCborConverter;

		@Nullable HttpMessageConverter<?> cborConverter;

		@Nullable HttpMessageConverter<?> yamlConverter;

		@Nullable HttpMessageConverter<?> protobufConverter;

		@Nullable HttpMessageConverter<?> atomConverter;

		@Nullable HttpMessageConverter<?> rssConverter;

		final List<HttpMessageConverter<?>> customConverters = new ArrayList<>();


		static {
			ClassLoader classLoader = DefaultBuilder.class.getClassLoader();
			JACKSON_PRESENT = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
			JACKSON_2_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
			GSON_PRESENT = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
			JSONB_PRESENT = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
			KOTLIN_SERIALIZATION_JSON_PRESENT = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
			JACKSON_SMILE_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
			JACKSON_2_SMILE_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
			JAXB_2_PRESENT = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
			JACKSON_XML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
			JACKSON_2_XML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
			JACKSON_CBOR_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
			JACKSON_2_CBOR_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
			JACKSON_YAML_PRESENT = JACKSON_PRESENT && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
			JACKSON_2_YAML_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
			KOTLIN_SERIALIZATION_CBOR_PRESENT = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
			KOTLIN_SERIALIZATION_PROTOBUF_PRESENT = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
			ROME_PRESENT = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		}


		void setStringConverter(HttpMessageConverter<?> stringConverter) {
			checkConverterSupports(stringConverter, MediaType.TEXT_PLAIN);
			this.stringConverter = stringConverter;
		}

		void setKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinJsonConverter) {
			Assert.notNull(kotlinJsonConverter, "kotlinJsonConverter must not be null");
			this.kotlinJsonConverter = kotlinJsonConverter;
		}

		void setJsonConverter(HttpMessageConverter<?> jsonConverter) {
			checkConverterSupports(jsonConverter, MediaType.APPLICATION_JSON);
			this.jsonConverter = jsonConverter;
		}

		void setXmlConverter(HttpMessageConverter<?> xmlConverter) {
			checkConverterSupports(xmlConverter, MediaType.TEXT_XML);
			this.xmlConverter = xmlConverter;
		}

		void setSmileConverter(HttpMessageConverter<?> smileConverter) {
			checkConverterSupports(smileConverter, new MediaType("application", "x-jackson-smile"));
			this.smileConverter = smileConverter;
		}

		void setKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinCborConverter) {
			Assert.notNull(kotlinCborConverter, "kotlinCborConverter must not be null");
			this.kotlinCborConverter = kotlinCborConverter;
		}

		void setCborConverter(HttpMessageConverter<?> cborConverter) {
			checkConverterSupports(cborConverter, MediaType.APPLICATION_CBOR);
			this.cborConverter = cborConverter;
		}

		void setYamlConverter(HttpMessageConverter<?> yamlConverter) {
			checkConverterSupports(yamlConverter, MediaType.APPLICATION_YAML);
			this.yamlConverter = yamlConverter;
		}

		private void checkConverterSupports(HttpMessageConverter<?> converter, MediaType mediaType) {
			for (MediaType supportedMediaType : converter.getSupportedMediaTypes()) {
				if (mediaType.equalsTypeAndSubtype(supportedMediaType)) {
					return;
				}
			}
			throw new IllegalArgumentException("converter should support '" + mediaType + "'");
		}

		void addCustomMessageConverter(HttpMessageConverter<?> customConverter) {
			Assert.notNull(customConverter, "'customConverter' must not be null");
			this.customConverters.add(customConverter);
		}

		void addMessageConverterConfigurer(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
		}

		List<HttpMessageConverter<?>> getBaseConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.byteArrayConverter != null) {
				converters.add(this.byteArrayConverter);
			}
			if (this.stringConverter != null) {
				converters.add(this.stringConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCoreConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.kotlinJsonConverter != null) {
				converters.add(this.kotlinJsonConverter);
			}
			if (this.jsonConverter != null) {
				converters.add(this.jsonConverter);
			}
			if (this.smileConverter != null) {
				converters.add(this.smileConverter);
			}
			if (this.kotlinCborConverter != null) {
				converters.add(this.kotlinCborConverter);
			}
			if (this.cborConverter != null) {
				converters.add(this.cborConverter);
			}
			if (this.yamlConverter != null) {
				converters.add(this.yamlConverter);
			}
			if (this.xmlConverter != null) {
				converters.add(this.xmlConverter);
			}
			if (this.protobufConverter != null) {
				converters.add(this.protobufConverter);
			}
			if (this.atomConverter != null) {
				converters.add(this.atomConverter);
			}
			if (this.rssConverter != null) {
				converters.add(this.rssConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCustomConverters() {
			return this.customConverters;
		}

		void detectMessageConverters() {
			this.byteArrayConverter = new ByteArrayHttpMessageConverter();

			if (this.stringConverter == null) {
				this.stringConverter = new StringHttpMessageConverter();
			}
			if (this.kotlinJsonConverter == null) {
				if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
					if (this.jsonConverter != null || JACKSON_PRESENT || JACKSON_2_PRESENT || GSON_PRESENT || JSONB_PRESENT) {
						this.kotlinJsonConverter = new KotlinSerializationJsonHttpMessageConverter();
					}
					else {
						this.kotlinJsonConverter = new KotlinSerializationJsonHttpMessageConverter(type -> true);
					}
				}
			}
			if (this.jsonConverter == null) {
				if (JACKSON_PRESENT) {
					this.jsonConverter = new JacksonJsonHttpMessageConverter();
				}
				else if (JACKSON_2_PRESENT) {
					this.jsonConverter = new MappingJackson2HttpMessageConverter();
				}
				else if (GSON_PRESENT) {
					this.jsonConverter = new GsonHttpMessageConverter();
				}
				else if (JSONB_PRESENT) {
					this.jsonConverter = new JsonbHttpMessageConverter();
				}
			}

			if (this.xmlConverter == null) {
				if (JACKSON_XML_PRESENT) {
					this.xmlConverter = new JacksonXmlHttpMessageConverter();
				}
				else if (JACKSON_2_XML_PRESENT) {
					this.xmlConverter = new MappingJackson2XmlHttpMessageConverter();
				}
				else if (JAXB_2_PRESENT) {
					this.xmlConverter = new Jaxb2RootElementHttpMessageConverter();
				}
			}

			if (this.smileConverter == null) {
				if (JACKSON_SMILE_PRESENT) {
					this.smileConverter = new JacksonSmileHttpMessageConverter();
				}
				else if (JACKSON_2_SMILE_PRESENT) {
					this.smileConverter = new MappingJackson2SmileHttpMessageConverter();
				}
			}

			if (this.kotlinCborConverter == null) {
				if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
					if (this.cborConverter != null || JACKSON_CBOR_PRESENT || JACKSON_2_CBOR_PRESENT) {
						this.kotlinCborConverter = new KotlinSerializationCborHttpMessageConverter();
					}
					else {
						this.kotlinCborConverter = new KotlinSerializationCborHttpMessageConverter(type -> true);
					}
				}
			}
			if (this.cborConverter == null) {
				if (JACKSON_CBOR_PRESENT) {
					this.cborConverter = new JacksonCborHttpMessageConverter();
				}
				else if (JACKSON_2_CBOR_PRESENT) {
					this.cborConverter = new MappingJackson2CborHttpMessageConverter();
				}
			}

			if (this.yamlConverter == null) {
				if (JACKSON_YAML_PRESENT) {
					this.yamlConverter = new JacksonYamlHttpMessageConverter();
				}
				else if (JACKSON_2_YAML_PRESENT) {
					this.yamlConverter = new MappingJackson2YamlHttpMessageConverter();
				}
			}

			if (this.protobufConverter == null) {
				if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
					this.protobufConverter = new KotlinSerializationProtobufHttpMessageConverter(type -> true);
				}
			}

			if (ROME_PRESENT) {
				if (this.atomConverter == null) {
					this.atomConverter = new AtomFeedHttpMessageConverter();
				}
				if (this.rssConverter == null) {
					this.rssConverter = new RssChannelHttpMessageConverter();
				}
			}
		}

	}

	static class DefaultClientBuilder extends DefaultBuilder implements ClientBuilder {

		@Override
		public DefaultClientBuilder registerDefaults() {
			this.registerDefaults = true;
			return this;
		}

		@Override
		public ClientBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
			setStringConverter(stringConverter);
			return this;
		}

		@Override
		public ClientBuilder withKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinSerializationJsonConverter) {
			setKotlinSerializationJsonConverter(kotlinSerializationJsonConverter);
			return this;
		}

		@Override
		public ClientBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
			setJsonConverter(jsonConverter);
			return this;
		}

		@Override
		public ClientBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
			setXmlConverter(xmlConverter);
			return this;
		}

		@Override
		public ClientBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
			setSmileConverter(smileConverter);
			return this;
		}

		@Override
		public ClientBuilder withKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinSerializationCborConverter) {
			setKotlinSerializationCborConverter(kotlinSerializationCborConverter);
			return this;
		}

		@Override
		public ClientBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
			setCborConverter(cborConverter);
			return this;
		}

		@Override
		public ClientBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
			setYamlConverter(yamlConverter);
			return this;
		}

		@Override
		public ClientBuilder addCustomConverter(HttpMessageConverter<?> customConverter) {
			addCustomMessageConverter(customConverter);
			return this;
		}

		@Override
		public ClientBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			addMessageConverterConfigurer(configurer);
			return this;
		}

		@Override
		public HttpMessageConverters build() {
			if (this.registerDefaults) {
				this.resourceConverter = new ResourceHttpMessageConverter(false);
				detectMessageConverters();
			}
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>(this.getCustomConverters());
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>(this.getCustomConverters());
			if (this.registerDefaults) {
				partConverters.addAll(this.getCoreConverters());
				allConverters.addAll(this.getBaseConverters());
				if (this.resourceConverter != null) {
					allConverters.add(this.resourceConverter);
				}
			}
			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			if (this.registerDefaults) {
				allConverters.addAll(this.getCoreConverters());
			}
			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return new DefaultHttpMessageConverters(allConverters);
		}
	}

	static class DefaultServerBuilder extends DefaultBuilder implements ServerBuilder {


		@Override
		public ServerBuilder registerDefaults() {
			this.registerDefaults = true;
			return this;
		}

		@Override
		public ServerBuilder withStringConverter(HttpMessageConverter<?> stringConverter) {
			setStringConverter(stringConverter);
			return this;
		}

		@Override
		public ServerBuilder withKotlinSerializationJsonConverter(HttpMessageConverter<?> kotlinSerializationJsonConverter) {
			setKotlinSerializationJsonConverter(kotlinSerializationJsonConverter);
			return this;
		}

		@Override
		public ServerBuilder withJsonConverter(HttpMessageConverter<?> jsonConverter) {
			setJsonConverter(jsonConverter);
			return this;
		}

		@Override
		public ServerBuilder withXmlConverter(HttpMessageConverter<?> xmlConverter) {
			setXmlConverter(xmlConverter);
			return this;
		}

		@Override
		public ServerBuilder withSmileConverter(HttpMessageConverter<?> smileConverter) {
			setSmileConverter(smileConverter);
			return this;
		}

		@Override
		public ServerBuilder withKotlinSerializationCborConverter(HttpMessageConverter<?> kotlinSerializationCborConverter) {
			setKotlinSerializationCborConverter(kotlinSerializationCborConverter);
			return this;
		}

		@Override
		public ServerBuilder withCborConverter(HttpMessageConverter<?> cborConverter) {
			setCborConverter(cborConverter);
			return this;
		}

		@Override
		public ServerBuilder withYamlConverter(HttpMessageConverter<?> yamlConverter) {
			setYamlConverter(yamlConverter);
			return this;
		}

		@Override
		public ServerBuilder addCustomConverter(HttpMessageConverter<?> customConverter) {
			addCustomMessageConverter(customConverter);
			return this;
		}

		@Override
		public ServerBuilder configureMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			addMessageConverterConfigurer(configurer);
			return this;
		}

		@Override
		public HttpMessageConverters build() {
			if (this.registerDefaults) {
				this.resourceConverter = new ResourceHttpMessageConverter();
				this.resourceRegionConverter = new ResourceRegionHttpMessageConverter();
				detectMessageConverters();
			}
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>(this.getCustomConverters());
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>(this.getCustomConverters());
			if (this.registerDefaults) {
				partConverters.addAll(this.getCoreConverters());
				allConverters.addAll(this.getBaseConverters());
				if (this.resourceConverter != null) {
					allConverters.add(this.resourceConverter);
				}
				if (this.resourceRegionConverter != null) {
					allConverters.add(this.resourceRegionConverter);
				}
			}
			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			if (this.registerDefaults) {
				allConverters.addAll(this.getCoreConverters());
			}
			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return new DefaultHttpMessageConverters(allConverters);
		}
	}

}
