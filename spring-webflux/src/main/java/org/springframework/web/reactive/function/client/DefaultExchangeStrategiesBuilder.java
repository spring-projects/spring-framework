/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

/**
 * Default implementation of {@link ExchangeStrategies.Builder}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
final class DefaultExchangeStrategiesBuilder implements ExchangeStrategies.Builder {

	final static ExchangeStrategies DEFAULT_EXCHANGE_STRATEGIES;

	static {
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();
		builder.defaultConfiguration();
		DEFAULT_EXCHANGE_STRATEGIES = builder.build();
	}


	private final ClientCodecConfigurer codecConfigurer = ClientCodecConfigurer.create();


	public DefaultExchangeStrategiesBuilder() {
		this.codecConfigurer.registerDefaults(false);
	}


	public void defaultConfiguration() {
		this.codecConfigurer.registerDefaults(true);
	}

	@Override
	public ExchangeStrategies.Builder codecs(Consumer<ClientCodecConfigurer> consumer) {
		consumer.accept(this.codecConfigurer);
		return this;
	}

	@Override
	public ExchangeStrategies build() {
		return new DefaultExchangeStrategies(
				this.codecConfigurer.getReaders(), this.codecConfigurer.getWriters());
	}


	private static class DefaultExchangeStrategies implements ExchangeStrategies {

		private final List<HttpMessageReader<?>> readers;

		private final List<HttpMessageWriter<?>> writers;


		public DefaultExchangeStrategies(List<HttpMessageReader<?>> readers, List<HttpMessageWriter<?>> writers) {
			this.readers = unmodifiableCopy(readers);
			this.writers = unmodifiableCopy(writers);
		}

		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}


		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.readers;
		}

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.writers;
		}
	}

}
