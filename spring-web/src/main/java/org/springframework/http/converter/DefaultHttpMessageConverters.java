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
				this.commonMessageConverters.registerDefaults(classLoader);
				this.clientMessageConverterConfigurer.registerDefaults(classLoader);
				this.serverMessageConverterConfigurer.registerDefaults(classLoader);
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
			this.commonMessageConverters.additionalMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public DefaultHttpMessageConverters build() {
			return new DefaultHttpMessageConverters(this.clientMessageConverterConfigurer.getMessageConverters(),
					this.serverMessageConverterConfigurer.getMessageConverters());
		}

	}

	static class DefaultMessageConverterConfigurer {

		private final @Nullable DefaultMessageConverterConfigurer inheritedMessageConverters;

		private @Nullable ByteArrayHttpMessageConverter byteArrayMessageConverter;

		private @Nullable HttpMessageConverter<?> stringMessageConverter;

		List<HttpMessageConverter<?>> resourceMessageConverters = Collections.emptyList();

		private @Nullable HttpMessageConverter<?> jsonMessageConverter;

		private @Nullable HttpMessageConverter<?> xmlMessageConverter;

		private @Nullable HttpMessageConverter<?> smileMessageConverter;

		private @Nullable HttpMessageConverter<?> cborMessageConverter;

		private @Nullable HttpMessageConverter<?> yamlMessageConverter;

		private final List<HttpMessageConverter<?>> additionalMessageConverters = new ArrayList<>();

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
			return converters;
		}

		List<HttpMessageConverter<?>> getCustomConverters() {
			List<HttpMessageConverter<?>> result = new ArrayList<>(this.additionalMessageConverters);
			if (this.inheritedMessageConverters != null) {
				result.addAll(this.inheritedMessageConverters.additionalMessageConverters);
			}
			return result;
		}

		void registerDefaults(ClassLoader classLoader) {
			this.byteArrayMessageConverter = new ByteArrayHttpMessageConverter();
			this.stringMessageConverter = new StringHttpMessageConverter();

			if (isJacksonPresent(classLoader)) {
				this.jsonMessageConverter = new JacksonJsonHttpMessageConverter();
			}
			else if (isJackson2Present(classLoader)) {
				this.jsonMessageConverter = new MappingJackson2HttpMessageConverter();
			}
			else if (isGsonPresent(classLoader)) {
				this.jsonMessageConverter = new GsonHttpMessageConverter();
			}
			else if (isJsonbPresent(classLoader)) {
				this.jsonMessageConverter = new JsonbHttpMessageConverter();
			}
			else if (isKotlinSerializationJsonPresent(classLoader)) {
				this.jsonMessageConverter = new KotlinSerializationJsonHttpMessageConverter();
			}

			if (isJacksonXmlPresent(classLoader)) {
				this.xmlMessageConverter = new JacksonXmlHttpMessageConverter();
			}
			else if (isJackson2XmlPresent(classLoader)) {
				this.xmlMessageConverter = new MappingJackson2XmlHttpMessageConverter();
			}
			else if (isJaxb2Present(classLoader)) {
				this.xmlMessageConverter = new Jaxb2RootElementHttpMessageConverter();
			}

			if (isJacksonSmilePresent(classLoader)) {
				this.smileMessageConverter = new JacksonSmileHttpMessageConverter();
			}
			else if (isJackson2SmilePresent(classLoader)) {
				this.smileMessageConverter = new MappingJackson2SmileHttpMessageConverter();
			}
			if (isJacksonCborPresent(classLoader)) {
				this.cborMessageConverter = new JacksonCborHttpMessageConverter();
			}
			else if (isJackson2CborPresent(classLoader)) {
				this.cborMessageConverter = new MappingJackson2CborHttpMessageConverter();
			}
			else if (isKotlinSerializationCborPresent(classLoader)) {
				this.cborMessageConverter = new KotlinSerializationCborHttpMessageConverter();
			}

			if (isJacksonYamlPresent(classLoader)) {
				this.yamlMessageConverter = new JacksonYamlHttpMessageConverter();
			}
			else if (isJackson2YamlPresent(classLoader)) {
				this.yamlMessageConverter = new MappingJackson2YamlHttpMessageConverter();
			}

			if (isKotlinSerializationProtobufPresent(classLoader)) {
				this.additionalMessageConverters.add(new KotlinSerializationProtobufHttpMessageConverter());
			}

			if (isRomePresent(classLoader)) {
				this.additionalMessageConverters.add(new AtomFeedHttpMessageConverter());
				this.additionalMessageConverters.add(new RssChannelHttpMessageConverter());
			}
		}

		private static boolean isRomePresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.rometools.rome.feed.WireFeed", classLoader);
		}

		private static boolean isJacksonPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
		}

		private static boolean isJackson2Present(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		}

		private static boolean isGsonPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		}

		private static boolean isJsonbPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
		}

		private static boolean isKotlinSerializationJsonPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
		}

		private static boolean isJacksonSmilePresent(ClassLoader classLoader) {
			return isJacksonPresent(classLoader) && ClassUtils.isPresent("tools.jackson.dataformat.smile.SmileMapper", classLoader);
		}

		private static boolean isJackson2SmilePresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		}

		private static boolean isJaxb2Present(ClassLoader classLoader) {
			return ClassUtils.isPresent("jakarta.xml.bind.Binder", classLoader);
		}

		private static boolean isJacksonXmlPresent(ClassLoader classLoader) {
			return isJacksonPresent(classLoader) && ClassUtils.isPresent("tools.jackson.dataformat.xml.XmlMapper", classLoader);
		}

		private static boolean isJackson2XmlPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.fasterxml.jackson.dataformat.xml.XmlMapper", classLoader);
		}

		private static boolean isJacksonCborPresent(ClassLoader classLoader) {
			return isJacksonPresent(classLoader) && ClassUtils.isPresent("tools.jackson.dataformat.cbor.CBORMapper", classLoader);
		}

		private static boolean isJackson2CborPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.fasterxml.jackson.dataformat.cbor.CBORFactory", classLoader);
		}

		private static boolean isJacksonYamlPresent(ClassLoader classLoader) {
			return isJacksonPresent(classLoader) && ClassUtils.isPresent("tools.jackson.dataformat.yaml.YAMLMapper", classLoader);
		}

		private static boolean isJackson2YamlPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("com.fasterxml.jackson.dataformat.yaml.YAMLFactory", classLoader);
		}

		private static boolean isKotlinSerializationCborPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("kotlinx.serialization.cbor.Cbor", classLoader);
		}

		private static boolean isKotlinSerializationProtobufPresent(ClassLoader classLoader) {
			return ClassUtils.isPresent("kotlinx.serialization.protobuf.ProtoBuf", classLoader);
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
			this.clientMessageConverters.additionalMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public ClientMessageConverterConfigurer configureClientMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
			return this;
		}

		@Override
		void registerDefaults(ClassLoader classLoader) {
			this.resourceMessageConverters = Collections.singletonList(new ResourceHttpMessageConverter(false));
		}

		List<HttpMessageConverter<?>> getMessageConverters() {
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

			partConverters.addAll(this.clientMessageConverters.getCoreConverters());
			partConverters.addAll(this.clientMessageConverters.getCustomConverters());

			allConverters.addAll(this.clientMessageConverters.getBaseConverters());
			allConverters.addAll(this.resourceMessageConverters);
			if (!partConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			allConverters.addAll(this.clientMessageConverters.getCoreConverters());
			allConverters.addAll(this.clientMessageConverters.getCustomConverters());

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
			this.serverMessageConverters.additionalMessageConverters.add(customConverter);
			return this;
		}

		@Override
		public ServerMessageConverterConfigurer configureServerMessageConverters(Consumer<HttpMessageConverter<?>> configurer) {
			this.configurer = (this.configurer != null) ? configurer.andThen(this.configurer) : configurer;
			return this;
		}

		@Override
		void registerDefaults(ClassLoader classLoader) {
			this.resourceMessageConverters = Arrays.asList(new ResourceHttpMessageConverter(), new ResourceRegionHttpMessageConverter());
		}

		List<HttpMessageConverter<?>> getMessageConverters() {
			List<HttpMessageConverter<?>> allConverters = new ArrayList<>();
			List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

			partConverters.addAll(this.serverMessageConverters.getCoreConverters());
			partConverters.addAll(this.serverMessageConverters.getCustomConverters());

			allConverters.addAll(this.serverMessageConverters.getBaseConverters());
			allConverters.addAll(this.resourceMessageConverters);
			allConverters.addAll(this.serverMessageConverters.getCoreConverters());
			allConverters.addAll(this.serverMessageConverters.getCustomConverters());
			if (!partConverters.isEmpty()) {
				allConverters.add(new AllEncompassingFormHttpMessageConverter(partConverters));
			}
			if (this.configurer != null) {
				allConverters.forEach(this.configurer);
			}
			return allConverters;
		}
	}

}
