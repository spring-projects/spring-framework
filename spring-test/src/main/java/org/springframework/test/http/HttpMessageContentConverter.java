/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.http;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * Convert HTTP message content for testing purposes.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class HttpMessageContentConverter {

	private static final MediaType JSON = MediaType.APPLICATION_JSON;

	private final List<HttpMessageConverter<?>> messageConverters;

	HttpMessageContentConverter(Iterable<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = StreamSupport.stream(messageConverters.spliterator(), false).toList();
		Assert.notEmpty(this.messageConverters, "At least one message converter needs to be specified");
	}


	/**
	 * Create an instance with an iterable of the candidates to use.
	 * @param candidates the candidates
	 */
	public static HttpMessageContentConverter of(Iterable<HttpMessageConverter<?>> candidates) {
		return new HttpMessageContentConverter(candidates);
	}

	/**
	 * Create an instance with a vararg of the candidates to use.
	 * @param candidates the candidates
	 */
	public static HttpMessageContentConverter of(HttpMessageConverter<?>... candidates) {
		return new HttpMessageContentConverter(Arrays.asList(candidates));
	}


	/**
	 * Convert the given {@link HttpInputMessage} whose content must match the
	 * given {@link MediaType} to the requested {@code targetType}.
	 * @param message an input message
	 * @param mediaType the media type of the input
	 * @param targetType the target type
	 * @param <T> the converted object type
	 * @return a value of the given {@code targetType}
	 */
	@SuppressWarnings("unchecked")
	public <T> T convert(HttpInputMessage message, MediaType mediaType, ResolvableType targetType)
			throws IOException, HttpMessageNotReadableException {
		Class<?> contextClass = targetType.getRawClass();
		SingletonSupplier<Type> javaType = SingletonSupplier.of(targetType::getType);
		for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
			if (messageConverter instanceof GenericHttpMessageConverter<?> genericMessageConverter) {
				Type type = javaType.obtain();
				if (genericMessageConverter.canRead(type, contextClass, mediaType)) {
					return (T) genericMessageConverter.read(type, contextClass, message);
				}
			}
			else if (messageConverter instanceof SmartHttpMessageConverter<?> smartMessageConverter) {
				if (smartMessageConverter.canRead(targetType, mediaType)) {
					return (T) smartMessageConverter.read(targetType, message, null);
				}
			}
			else {
				Class<?> targetClass = (contextClass != null ? contextClass : Object.class);
				if (messageConverter.canRead(targetClass, mediaType)) {
					HttpMessageConverter<T> simpleMessageConverter = (HttpMessageConverter<T>) messageConverter;
					Class<? extends T> clazz = (Class<? extends T>) targetClass;
					return simpleMessageConverter.read(clazz, message);
				}
			}
		}
		throw new IllegalStateException("No converter found to read [%s] to [%s]".formatted(mediaType, targetType));
	}

	/**
	 * Convert the given raw value to the given {@code targetType} by writing
	 * it first to JSON and reading it back.
	 * @param value the value to convert
	 * @param targetType the target type
	 * @param <T> the converted object type
	 * @return a value of the given {@code targetType}
	 */
	public <T> T convertViaJson(Object value, ResolvableType targetType) throws IOException {
		MockHttpOutputMessage outputMessage = convertToJson(value, ResolvableType.forInstance(value));
		return convert(fromHttpOutputMessage(outputMessage), JSON, targetType);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private MockHttpOutputMessage convertToJson(Object value, ResolvableType valueType) throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		Class<?> valueClass = value.getClass();
		SingletonSupplier<Type> javaType = SingletonSupplier.of(valueType::getType);
		for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
			if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
				Type type = javaType.obtain();
				if (genericMessageConverter.canWrite(type, valueClass, JSON)) {
					genericMessageConverter.write(value, type, JSON, outputMessage);
					return outputMessage;
				}
			}
			else if (messageConverter instanceof SmartHttpMessageConverter smartMessageConverter) {
				if (smartMessageConverter.canWrite(valueType, valueClass, JSON)) {
					smartMessageConverter.write(value, valueType, JSON, outputMessage, null);
					return outputMessage;
				}
			}
			else if (messageConverter.canWrite(valueClass, JSON)) {
				((HttpMessageConverter<Object>) messageConverter).write(value, JSON, outputMessage);
				return outputMessage;
			}
		}
		throw new IllegalStateException("No converter found to convert [%s] to JSON".formatted(valueType));
	}

	private static HttpInputMessage fromHttpOutputMessage(MockHttpOutputMessage message) {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(message.getBodyAsBytes());
		inputMessage.getHeaders().addAll(message.getHeaders());
		return inputMessage;
	}

}
