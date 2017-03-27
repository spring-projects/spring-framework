/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link ExchangeStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultExchangeStrategiesBuilder implements ExchangeStrategies.Builder {

	private final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();

	private final List<HttpMessageWriter<?>> messageWriters = new ArrayList<>();


	public void defaultConfiguration() {
		ClientCodecConfigurer configurer = new ClientCodecConfigurer();
		configurer.getReaders().forEach(this::messageReader);
		configurer.getWriters().forEach(this::messageWriter);
	}

	public void applicationContext(ApplicationContext applicationContext) {
		applicationContext.getBeansOfType(HttpMessageReader.class).values().forEach(this::messageReader);
		applicationContext.getBeansOfType(HttpMessageWriter.class).values().forEach(this::messageWriter);
	}

	@Override
	public ExchangeStrategies.Builder messageReader(HttpMessageReader<?> messageReader) {
		Assert.notNull(messageReader, "'messageReader' must not be null");
		this.messageReaders.add(messageReader);
		return this;
	}

	@Override
	public ExchangeStrategies.Builder decoder(Decoder<?> decoder) {
		Assert.notNull(decoder, "'decoder' must not be null");
		return messageReader(new DecoderHttpMessageReader<>(decoder));
	}

	@Override
	public ExchangeStrategies.Builder messageWriter(HttpMessageWriter<?> messageWriter) {
		Assert.notNull(messageWriter, "'messageWriter' must not be null");
		this.messageWriters.add(messageWriter);
		return this;
	}

	@Override
	public ExchangeStrategies.Builder encoder(Encoder<?> encoder) {
		Assert.notNull(encoder, "'encoder' must not be null");
		return messageWriter(new EncoderHttpMessageWriter<>(encoder));
	}

	@Override
	public ExchangeStrategies build() {
		return new DefaultExchangeStrategies(this.messageReaders, this.messageWriters);
	}


	private static class DefaultExchangeStrategies implements ExchangeStrategies {

		private final List<HttpMessageReader<?>> messageReaders;

		private final List<HttpMessageWriter<?>> messageWriters;

		public DefaultExchangeStrategies(
				List<HttpMessageReader<?>> messageReaders, List<HttpMessageWriter<?>> messageWriters) {

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
