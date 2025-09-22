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
 */
@SuppressWarnings("removal")
class DefaultHttpMessageConverters implements HttpMessageConverters {

	private final List<HttpMessageConverter<?>> messageConverters;

	DefaultHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
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

		@Nullable ByteArrayHttpMessageConverter byteArrayMessageConverter;

		@Nullable HttpMessageConverter<?> stringMessageConverter;

		@Nullable HttpMessageConverter<?> resourceMessageConverter;

		@Nullable HttpMessageConverter<?> resourceRegionMessageConverter;

		@Nullable Consumer<HttpMessageConverter<?>> configurer;

		@Nullable HttpMessageConverter<?> jsonMessageConverter;

		@Nullable HttpMessageConverter<?> xmlMessageConverter;

		@Nullable HttpMessageConverter<?> smileMessageConverter;

		@Nullable HttpMessageConverter<?> cborMessageConverter;

		@Nullable HttpMessageConverter<?> yamlMessageConverter;

		@Nullable HttpMessageConverter<?> protobufMessageConverter;

		@Nullable HttpMessageConverter<?> atomMessageConverter;

		@Nullable HttpMessageConverter<?> rssMessageConverter;

		final List<HttpMessageConverter<?>> customMessageConverters = new ArrayList<>();


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


		void setStringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			Assert.isTrue(stringMessageConverter.getSupportedMediaTypes().contains(MediaType.TEXT_PLAIN),
			"stringMessageConverter should support 'text/plain'");
			this.stringMessageConverter = stringMessageConverter;
		}

		void setJsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			Assert.isTrue(jsonMessageConverter.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON),
					"jsonMessageConverter should support 'application/json'");
			this.jsonMessageConverter = jsonMessageConverter;
		}

		void setXmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			Assert.isTrue(xmlMessageConverter.getSupportedMediaTypes().contains(MediaType.TEXT_XML),
					"xmlMessageConverter should support 'text/xml'");
			this.xmlMessageConverter = xmlMessageConverter;
		}

		void setSmileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			Assert.isTrue(smileMessageConverter.getSupportedMediaTypes().contains(new MediaType("application", "x-jackson-smile")),
					"smileMessageConverter should support 'application/x-jackson-smile'");
			this.smileMessageConverter = smileMessageConverter;
		}

		void setCborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			Assert.isTrue(cborMessageConverter.getSupportedMediaTypes().contains(MediaType.APPLICATION_CBOR),
					"cborMessageConverter should support 'application/cbor'");
			this.cborMessageConverter = cborMessageConverter;
		}

		void setYamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			Assert.isTrue(yamlMessageConverter.getSupportedMediaTypes().contains(MediaType.APPLICATION_YAML),
					"yamlMessageConverter should support 'application/yaml'");
			this.yamlMessageConverter = yamlMessageConverter;
		}

		void addCustomMessageConverter(HttpMessageConverter<?> customConverter) {
			Assert.notNull(customConverter, "'customConverter' must not be null");
			this.customMessageConverters.add(customConverter);
		}

		void addMessageConverterConfigurer(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
		}

		List<HttpMessageConverter<?>> getBaseConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.byteArrayMessageConverter != null) {
				converters.add(this.byteArrayMessageConverter);
			}
			if (this.stringMessageConverter != null) {
				converters.add(this.stringMessageConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCoreConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.jsonMessageConverter != null) {
				converters.add(this.jsonMessageConverter);
			}
			if (this.smileMessageConverter != null) {
				converters.add(this.smileMessageConverter);
			}
			if (this.cborMessageConverter!= null) {
				converters.add(this.cborMessageConverter);
			}
			if (this.yamlMessageConverter!= null) {
				converters.add(this.yamlMessageConverter);
			}
			if (this.xmlMessageConverter!= null) {
				converters.add(this.xmlMessageConverter);
			}
			if (this.protobufMessageConverter != null) {
				converters.add(this.protobufMessageConverter);
			}
			if (this.atomMessageConverter != null) {
				converters.add(this.atomMessageConverter);
			}
			if (this.rssMessageConverter != null) {
				converters.add(this.rssMessageConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCustomConverters() {
			return this.customMessageConverters;
		}

		void detectMessageConverters() {
			this.byteArrayMessageConverter = new ByteArrayHttpMessageConverter();
			this.stringMessageConverter = new StringHttpMessageConverter();

			if (this.jsonMessageConverter == null) {
				if (JACKSON_PRESENT) {
					this.jsonMessageConverter = new JacksonJsonHttpMessageConverter();
				}
				else if (JACKSON_2_PRESENT) {
					this.jsonMessageConverter = new MappingJackson2HttpMessageConverter();
				}
				else if (GSON_PRESENT) {
					this.jsonMessageConverter = new GsonHttpMessageConverter();
				}
				else if (JSONB_PRESENT) {
					this.jsonMessageConverter = new JsonbHttpMessageConverter();
				}
				else if (KOTLIN_SERIALIZATION_JSON_PRESENT) {
					this.jsonMessageConverter = new KotlinSerializationJsonHttpMessageConverter();
				}
			}

			if (this.xmlMessageConverter == null) {
				if (JACKSON_XML_PRESENT) {
					this.xmlMessageConverter = new JacksonXmlHttpMessageConverter();
				}
				else if (JACKSON_2_XML_PRESENT) {
					this.xmlMessageConverter = new MappingJackson2XmlHttpMessageConverter();
				}
				else if (JAXB_2_PRESENT) {
					this.xmlMessageConverter = new Jaxb2RootElementHttpMessageConverter();
				}
			}

			if (this.smileMessageConverter == null) {
				if (JACKSON_SMILE_PRESENT) {
					this.smileMessageConverter = new JacksonSmileHttpMessageConverter();
				}
				else if (JACKSON_2_SMILE_PRESENT) {
					this.smileMessageConverter = new MappingJackson2SmileHttpMessageConverter();
				}
			}

			if (this.cborMessageConverter == null) {
				if (JACKSON_CBOR_PRESENT) {
					this.cborMessageConverter = new JacksonCborHttpMessageConverter();
				}
				else if (JACKSON_2_CBOR_PRESENT) {
					this.cborMessageConverter = new MappingJackson2CborHttpMessageConverter();
				}
				else if (KOTLIN_SERIALIZATION_CBOR_PRESENT) {
					this.cborMessageConverter = new KotlinSerializationCborHttpMessageConverter();
				}
			}

			if (this.yamlMessageConverter == null) {
				if (JACKSON_YAML_PRESENT) {
					this.yamlMessageConverter = new JacksonYamlHttpMessageConverter();
				}
				else if (JACKSON_2_YAML_PRESENT) {
					this.yamlMessageConverter = new MappingJackson2YamlHttpMessageConverter();
				}
			}

			if (this.protobufMessageConverter == null) {
				if (KOTLIN_SERIALIZATION_PROTOBUF_PRESENT) {
					this.protobufMessageConverter = new KotlinSerializationProtobufHttpMessageConverter();
				}
			}

			if (ROME_PRESENT) {
				if (this.atomMessageConverter == null) {
					this.atomMessageConverter = new AtomFeedHttpMessageConverter();
				}
				if (this.rssMessageConverter == null) {
					this.rssMessageConverter = new RssChannelHttpMessageConverter();
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
		public ClientBuilder stringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			setStringMessageConverter(stringMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			setJsonMessageConverter(jsonMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			setXmlMessageConverter(xmlMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder smileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			setSmileMessageConverter(smileMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder cborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			setCborMessageConverter(cborMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			setYamlMessageConverter(yamlMessageConverter);
			return this;
		}

		@Override
		public ClientBuilder customMessageConverter(HttpMessageConverter<?> customConverter) {
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
				this.resourceMessageConverter = new ResourceHttpMessageConverter(false);
				detectMessageConverters();
			}
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();
			partConverters.addAll(this.getCustomConverters());
			partConverters.addAll(this.getCoreConverters());

			allConverters.addAll(this.getCustomConverters());
			allConverters.addAll(this.getBaseConverters());
			if (this.resourceMessageConverter != null) {
				allConverters.add(this.resourceMessageConverter);
			}
			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			allConverters.addAll(this.getCoreConverters());
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
		public ServerBuilder stringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			setStringMessageConverter(stringMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			setJsonMessageConverter(jsonMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			setXmlMessageConverter(xmlMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder smileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			setSmileMessageConverter(smileMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder cborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			setCborMessageConverter(cborMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			setYamlMessageConverter(yamlMessageConverter);
			return this;
		}

		@Override
		public ServerBuilder customMessageConverter(HttpMessageConverter<?> customConverter) {
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
				this.resourceMessageConverter = new ResourceHttpMessageConverter();
				this.resourceRegionMessageConverter = new ResourceRegionHttpMessageConverter();
				detectMessageConverters();
			}
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

			partConverters.addAll(this.getCustomConverters());
			partConverters.addAll(this.getCoreConverters());

			allConverters.addAll(this.getCustomConverters());
			allConverters.addAll(this.getBaseConverters());
			if (this.resourceMessageConverter != null) {
				allConverters.add(this.resourceMessageConverter);
			}
			if (this.resourceRegionMessageConverter != null) {
				allConverters.add(this.resourceRegionMessageConverter);
			}
			if (!partConverters.isEmpty() || !allConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			allConverters.addAll(this.getCoreConverters());
			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return new DefaultHttpMessageConverters(allConverters);
		}
	}

}
