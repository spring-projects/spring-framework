/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ResourceHttpMessageWriter;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Default implementation of {@link WebClientStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultWebClientStrategiesBuilder implements WebClientStrategies.Builder {

	private static final boolean jackson2Present =
			ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper",
					DefaultWebClientStrategiesBuilder.class.getClassLoader()) &&
					ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
							DefaultWebClientStrategiesBuilder.class.getClassLoader());

	private static final boolean jaxb2Present =
			ClassUtils.isPresent("javax.xml.bind.Binder",
					DefaultWebClientStrategiesBuilder.class.getClassLoader());


	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();


	public void defaultConfiguration() {
		messageReader(new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		messageReader(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		messageReader(new DecoderHttpMessageReader<>(new StringDecoder(false)));

		messageWriter(new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		messageWriter(new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		messageWriter(new EncoderHttpMessageWriter<>(new CharSequenceEncoder()));
		messageWriter(new ResourceHttpMessageWriter());

		if (jaxb2Present) {
			messageReader(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
			messageWriter(new EncoderHttpMessageWriter<>(new Jaxb2XmlEncoder()));
		}
		if (jackson2Present) {
			messageReader(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));
			messageWriter(new EncoderHttpMessageWriter<>(new Jackson2JsonEncoder()));
		}
	}

	public void applicationContext(ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(HttpMessageReader.class).values().forEach(this::messageReader);
		applicationContext.getBeansOfType(HttpMessageWriter.class).values().forEach(this::messageWriter);
	}

	@Override
	public WebClientStrategies.Builder messageReader(HttpMessageReader<?> messageReader) {
		Assert.notNull(messageReader, "'messageReader' must not be null");
		this.messageReaders.add(messageReader);
		return this;
	}

	@Override
	public WebClientStrategies.Builder decoder(Decoder<?> decoder) {
		Assert.notNull(decoder, "'decoder' must not be null");
		return messageReader(new DecoderHttpMessageReader<>(decoder));
	}

	@Override
	public WebClientStrategies.Builder messageWriter(HttpMessageWriter<?> messageWriter) {
		Assert.notNull(messageWriter, "'messageWriter' must not be null");
		this.messageWriters.add(messageWriter);
		return this;
	}

	@Override
	public WebClientStrategies.Builder encoder(Encoder<?> encoder) {
		Assert.notNull(encoder, "'encoder' must not be null");
		return messageWriter(new EncoderHttpMessageWriter<>(encoder));
	}

	@Override
	public WebClientStrategies build() {
		return new DefaultWebClientStrategies(this.messageReaders, this.messageWriters);
	}

	private static class DefaultWebClientStrategies implements WebClientStrategies {

		private final List<HttpMessageReader<?>> messageReaders;

		private final List<HttpMessageWriter<?>> messageWriters;

		public DefaultWebClientStrategies(
				List<HttpMessageReader<?>> messageReaders,
				List<HttpMessageWriter<?>> messageWriters) {
			this.messageReaders = unmodifiableCopy(messageReaders);
			this.messageWriters = unmodifiableCopy(messageWriters);
		}

		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		@Override
		public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
			return this.messageReaders::stream;
		}

		@Override
		public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
			return this.messageWriters::stream;
		}

	}

}
