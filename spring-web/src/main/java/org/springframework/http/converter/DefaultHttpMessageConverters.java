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
import java.util.Arrays;
import java.util.Collections;
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

	private final List<HttpMessageConverter<?>> clientMessageConverters;

	private final List<HttpMessageConverter<?>> serverMessageConverters;

	DefaultHttpMessageConverters(List<HttpMessageConverter<?>> clientMessageConverters, List<HttpMessageConverter<?>> serverMessageConverters) {
		this.clientMessageConverters = clientMessageConverters;
		this.serverMessageConverters = serverMessageConverters;
	}

	@Override
	public Iterable<HttpMessageConverter<?>> forClient() {
		return this.clientMessageConverters;
	}

	@Override
	public Iterable<HttpMessageConverter<?>> forServer() {
		return this.serverMessageConverters;
	}

	static class DefaultBuilder implements HttpMessageConverters.Builder {

		private final DefaultMessageConverterConfigurer commonMessageConverters;

		private final DefaultClientMessageConverterConfigurer clientMessageConverterConfigurer;

		private final DefaultServerMessageConverterConfigurer serverMessageConverterConfigurer;


		DefaultBuilder(boolean registerDefaults) {
			this(registerDefaults, DefaultHttpMessageConverters.class.getClassLoader());
		}

		DefaultBuilder(boolean registerDefaults, ClassLoader classLoader) {
			this.commonMessageConverters = new DefaultMessageConverterConfigurer();
			this.clientMessageConverterConfigurer = new DefaultClientMessageConverterConfigurer(this.commonMessageConverters);
			this.serverMessageConverterConfigurer = new DefaultServerMessageConverterConfigurer(this.commonMessageConverters);
			if (registerDefaults) {
				this.commonMessageConverters.registerDefaults();
				this.clientMessageConverterConfigurer.registerDefaults();
				this.serverMessageConverterConfigurer.registerDefaults();
			}
		}

		@Override
		public Builder configureClient(Consumer<ClientMessageConverterConfigurer> consumer) {
			consumer.accept(this.clientMessageConverterConfigurer);
			return this;
		}

		@Override
		public Builder configureServer(Consumer<ServerMessageConverterConfigurer> consumer) {
			consumer.accept(this.serverMessageConverterConfigurer);
			return this;
		}

		@Override
		public Builder stringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			this.commonMessageConverters.setStringMessageConverter(stringMessageConverter);
			return this;
		}

		@Override
		public DefaultBuilder jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			this.commonMessageConverters.setJsonMessageConverter(jsonMessageConverter);
			return this;
		}

		@Override
		public DefaultBuilder xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			this.commonMessageConverters.setXmlMessageConverter(xmlMessageConverter);
			return this;
		}

		@Override
		public DefaultBuilder smileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			this.commonMessageConverters.setSmileMessageConverter(smileMessageConverter);
			return this;
		}

		@Override
		public Builder cborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			this.commonMessageConverters.setCborMessageConverter(cborMessageConverter);
			return this;
		}

		@Override
		public Builder yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			this.commonMessageConverters.setYamlMessageConverter(yamlMessageConverter);
			return this;
		}

		@Override
		public DefaultBuilder additionalMessageConverter(HttpMessageConverter<?> customConverter) {
			Assert.notNull(customConverter, "'customConverter' must not be null");
			this.commonMessageConverters.customMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public DefaultHttpMessageConverters build() {
			return new DefaultHttpMessageConverters(this.clientMessageConverterConfigurer.getMessageConverters(),
					this.serverMessageConverterConfigurer.getMessageConverters());
		}

	}

	static class DefaultMessageConverterConfigurer {

		private static final boolean isJacksonPresent;

		private static final boolean isJackson2Present;

		private static final boolean isGsonPresent;

		private static final boolean isJsonbPresent;

		private static final boolean isKotlinSerializationJsonPresent;

		private static final boolean isJacksonXmlPresent;

		private static final boolean isJackson2XmlPresent;

		private static final boolean isJaxb2Present;

		private static final boolean isJacksonSmilePresent;

		private static final boolean isJackson2SmilePresent;

		private static final boolean isJacksonCborPresent;

		private static final boolean isJackson2CborPresent;

		private static final boolean isKotlinSerializationCborPresent;

		private static final boolean isJacksonYamlPresent;

		private static final boolean isJackson2YamlPresent;

		private static final boolean isKotlinSerializationProtobufPresent;

		private static final boolean isRomePresent;


		private final @Nullable DefaultMessageConverterConfigurer inheritedMessageConverters;

		private @Nullable ByteArrayHttpMessageConverter byteArrayMessageConverter;

		private @Nullable HttpMessageConverter<?> stringMessageConverter;

		List<HttpMessageConverter<?>> resourceMessageConverters = Collections.emptyList();

		private @Nullable HttpMessageConverter<?> jsonMessageConverter;

		private @Nullable HttpMessageConverter<?> xmlMessageConverter;

		private @Nullable HttpMessageConverter<?> smileMessageConverter;

		private @Nullable HttpMessageConverter<?> cborMessageConverter;

		private @Nullable HttpMessageConverter<?> yamlMessageConverter;

		private @Nullable HttpMessageConverter<?> protobufMessageConverter;

		private @Nullable HttpMessageConverter<?> atomMessageConverter;

		private @Nullable HttpMessageConverter<?> rssMessageConverter;

		private final List<HttpMessageConverter<?>> customMessageConverters = new ArrayList<>();

		static {
			ClassLoader classLoader = DefaultClientMessageConverterConfigurer.class.getClassLoader();
			isJacksonPresent = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
			isJackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
						ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
			isGsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
			isJsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
			isKotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
			isJacksonSmilePresent = isJacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
			isJackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
			isJaxb2Present = ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
			isJacksonXmlPresent = isJacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
			isJackson2XmlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
			isJacksonCborPresent = isJacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
			isJackson2CborPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
			isJacksonYamlPresent = isJacksonPresent && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
			isJackson2YamlPresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
			isKotlinSerializationCborPresent = ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
			isKotlinSerializationProtobufPresent = ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
			isRomePresent = ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		}

		DefaultMessageConverterConfigurer() {
			this(null);
		}

		DefaultMessageConverterConfigurer(@Nullable DefaultMessageConverterConfigurer inheritedMessageConverters) {
			this.inheritedMessageConverters = inheritedMessageConverters;
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

		List<HttpMessageConverter<?>> getBaseConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.byteArrayMessageConverter != null) {
				converters.add(this.byteArrayMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.byteArrayMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.byteArrayMessageConverter);
			}
			if (this.stringMessageConverter != null) {
				converters.add(this.stringMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.stringMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.stringMessageConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCoreConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<>();
			if (this.jsonMessageConverter != null) {
				converters.add(this.jsonMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.jsonMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.jsonMessageConverter);
			}
			if (this.smileMessageConverter != null) {
				converters.add(this.smileMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.smileMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.smileMessageConverter);
			}
			if (this.cborMessageConverter!= null) {
				converters.add(this.cborMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.cborMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.cborMessageConverter);
			}
			if (this.yamlMessageConverter!= null) {
				converters.add(this.yamlMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.yamlMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.yamlMessageConverter);
			}
			if (this.xmlMessageConverter!= null) {
				converters.add(this.xmlMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.xmlMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.xmlMessageConverter);
			}
			if (this.protobufMessageConverter != null) {
				converters.add(this.protobufMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.protobufMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.protobufMessageConverter);
			}
			if (this.atomMessageConverter != null) {
				converters.add(this.atomMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.atomMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.atomMessageConverter);
			}
			if (this.rssMessageConverter != null) {
				converters.add(this.rssMessageConverter);
			}
			else if (this.inheritedMessageConverters != null &&
					this.inheritedMessageConverters.rssMessageConverter != null) {
				converters.add(this.inheritedMessageConverters.rssMessageConverter);
			}
			return converters;
		}

		List<HttpMessageConverter<?>> getCustomConverters() {
			List<HttpMessageConverter<?>> result = new ArrayList<>(this.customMessageConverters);
			if (this.inheritedMessageConverters != null) {
				result.addAll(this.inheritedMessageConverters.customMessageConverters);
			}
			return result;
		}

		void registerDefaults() {
			this.byteArrayMessageConverter = new ByteArrayHttpMessageConverter();
			this.stringMessageConverter = new StringHttpMessageConverter();

			if (isJacksonPresent) {
				this.jsonMessageConverter = new JacksonJsonHttpMessageConverter();
			}
			else if (isJackson2Present) {
				this.jsonMessageConverter = new MappingJackson2HttpMessageConverter();
			}
			else if (isGsonPresent) {
				this.jsonMessageConverter = new GsonHttpMessageConverter();
			}
			else if (isJsonbPresent) {
				this.jsonMessageConverter = new JsonbHttpMessageConverter();
			}
			else if (isKotlinSerializationJsonPresent) {
				this.jsonMessageConverter = new KotlinSerializationJsonHttpMessageConverter();
			}

			if (isJacksonXmlPresent) {
				this.xmlMessageConverter = new JacksonXmlHttpMessageConverter();
			}
			else if (isJackson2XmlPresent) {
				this.xmlMessageConverter = new MappingJackson2XmlHttpMessageConverter();
			}
			else if (isJaxb2Present) {
				this.xmlMessageConverter = new Jaxb2RootElementHttpMessageConverter();
			}

			if (isJacksonSmilePresent) {
				this.smileMessageConverter = new JacksonSmileHttpMessageConverter();
			}
			else if (isJackson2SmilePresent) {
				this.smileMessageConverter = new MappingJackson2SmileHttpMessageConverter();
			}
			if (isJacksonCborPresent) {
				this.cborMessageConverter = new JacksonCborHttpMessageConverter();
			}
			else if (isJackson2CborPresent) {
				this.cborMessageConverter = new MappingJackson2CborHttpMessageConverter();
			}
			else if (isKotlinSerializationCborPresent) {
				this.cborMessageConverter = new KotlinSerializationCborHttpMessageConverter();
			}

			if (isJacksonYamlPresent) {
				this.yamlMessageConverter = new JacksonYamlHttpMessageConverter();
			}
			else if (isJackson2YamlPresent) {
				this.yamlMessageConverter = new MappingJackson2YamlHttpMessageConverter();
			}

			if (isKotlinSerializationProtobufPresent) {
				this.protobufMessageConverter = new KotlinSerializationProtobufHttpMessageConverter();
			}

			if (isRomePresent) {
				this.atomMessageConverter = new AtomFeedHttpMessageConverter();
				this.rssMessageConverter = new RssChannelHttpMessageConverter();
			}
		}

	}

	static class DefaultClientMessageConverterConfigurer extends DefaultMessageConverterConfigurer implements ClientMessageConverterConfigurer {

		private @Nullable Consumer<HttpMessageConverter<?>> configurer;

		private final DefaultMessageConverterConfigurer clientMessageConverters;


		public DefaultClientMessageConverterConfigurer(DefaultMessageConverterConfigurer parentMessageConverters) {
			this.clientMessageConverters = new DefaultMessageConverterConfigurer(parentMessageConverters);
		}

		@Override
		public ClientMessageConverterConfigurer stringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			this.clientMessageConverters.setStringMessageConverter(stringMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			this.clientMessageConverters.setJsonMessageConverter(jsonMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			this.clientMessageConverters.setXmlMessageConverter(xmlMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer smileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			this.clientMessageConverters.setSmileMessageConverter(smileMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer cborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			this.clientMessageConverters.setCborMessageConverter(cborMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			this.clientMessageConverters.setYamlMessageConverter(yamlMessageConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer additionalMessageConverter(HttpMessageConverter<?> customConverter) {
			Assert.notNull(customConverter, "'customConverter' must not be null");
			this.clientMessageConverters.customMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer configureClientMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
			return this;
		}

		@Override
		void registerDefaults() {
			this.resourceMessageConverters = Collections.singletonList(new ResourceHttpMessageConverter(false));
		}

		List<HttpMessageConverter<?>> getMessageConverters() {
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

			partConverters.addAll(this.clientMessageConverters.getCustomConverters());
			partConverters.addAll(this.clientMessageConverters.getCoreConverters());

			allConverters.addAll(this.clientMessageConverters.getCustomConverters());
			allConverters.addAll(this.clientMessageConverters.getBaseConverters());
			allConverters.addAll(this.resourceMessageConverters);
			if (!partConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			allConverters.addAll(this.clientMessageConverters.getCoreConverters());

			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return allConverters;
		}
	}

	static class DefaultServerMessageConverterConfigurer extends DefaultMessageConverterConfigurer implements ServerMessageConverterConfigurer {

		private @Nullable Consumer<HttpMessageConverter<?>> configurer;

		private final DefaultMessageConverterConfigurer serverMessageConverters;


		DefaultServerMessageConverterConfigurer(DefaultMessageConverterConfigurer commonMessageConverters) {
			this.serverMessageConverters = new DefaultMessageConverterConfigurer(commonMessageConverters);
		}

		@Override
		public ServerMessageConverterConfigurer stringMessageConverter(HttpMessageConverter<?> stringMessageConverter) {
			this.serverMessageConverters.setStringMessageConverter(stringMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer jsonMessageConverter(HttpMessageConverter<?> jsonMessageConverter) {
			this.serverMessageConverters.setJsonMessageConverter(jsonMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer xmlMessageConverter(HttpMessageConverter<?> xmlMessageConverter) {
			this.serverMessageConverters.setXmlMessageConverter(xmlMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer smileMessageConverter(HttpMessageConverter<?> smileMessageConverter) {
			this.serverMessageConverters.setSmileMessageConverter(smileMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer cborMessageConverter(HttpMessageConverter<?> cborMessageConverter) {
			this.serverMessageConverters.setCborMessageConverter(cborMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer yamlMessageConverter(HttpMessageConverter<?> yamlMessageConverter) {
			this.serverMessageConverters.setYamlMessageConverter(yamlMessageConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer additionalMessageConverter(HttpMessageConverter<?> customConverter) {
			Assert.notNull(customConverter, "'customConverter' must not be null");
			this.serverMessageConverters.customMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer configureServerMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
			return this;
		}

		@Override
		void registerDefaults() {
			this.resourceMessageConverters = Arrays.asList(new ResourceHttpMessageConverter(), new ResourceRegionHttpMessageConverter());
		}

		List<HttpMessageConverter<?>> getMessageConverters() {
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

			partConverters.addAll(this.serverMessageConverters.getCustomConverters());
			partConverters.addAll(this.serverMessageConverters.getCoreConverters());

			allConverters.addAll(this.serverMessageConverters.getCustomConverters());
			allConverters.addAll(this.serverMessageConverters.getBaseConverters());
			allConverters.addAll(this.resourceMessageConverters);
			if (!partConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			allConverters.addAll(this.serverMessageConverters.getCoreConverters());
			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return allConverters;
		}
	}

}
