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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ExchangeStrategiesTests {

	@Test
	public void empty() {
		ExchangeStrategies strategies = ExchangeStrategies.empty().build();
		assertEquals(Optional.empty(), strategies.messageReaders().get().findFirst());
		assertEquals(Optional.empty(), strategies.messageWriters().get().findFirst());
	}

	@Test
	public void ofSuppliers() {
		HttpMessageReader<?> messageReader = new DummyMessageReader();
		HttpMessageWriter<?> messageWriter = new DummyMessageWriter();

		ExchangeStrategies strategies = ExchangeStrategies.of(
				() -> Stream.of(messageReader),
				() -> Stream.of(messageWriter));

		assertEquals(1L, strategies.messageReaders().get().collect(Collectors.counting()).longValue());
		assertEquals(Optional.of(messageReader), strategies.messageReaders().get().findFirst());

		assertEquals(1L, strategies.messageWriters().get().collect(Collectors.counting()).longValue());
		assertEquals(Optional.of(messageWriter), strategies.messageWriters().get().findFirst());
	}

	@Test
	public void toConfiguration() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageWriter", DummyMessageWriter.class);
		applicationContext.registerSingleton("messageReader", DummyMessageReader.class);
		applicationContext.refresh();

		ExchangeStrategies strategies = ExchangeStrategies.of(applicationContext);
		assertTrue(strategies.messageReaders().get()
				.allMatch(r -> r instanceof DummyMessageReader));
		assertTrue(strategies.messageWriters().get()
				.allMatch(r -> r instanceof DummyMessageWriter));

	}


	private static class DummyMessageWriter implements HttpMessageWriter<Object> {

		@Override
		public boolean canWrite(ResolvableType type, MediaType mediaType) {
			return false;
		}

		@Override
		public List<MediaType> getWritableMediaTypes() {
			return Collections.emptyList();
		}

		@Override
		public Mono<Void> write(Publisher<?> inputStream, ResolvableType type,
				MediaType contentType,
				ReactiveHttpOutputMessage outputMessage,
				Map<String, Object> hints) {
			return Mono.empty();
		}
	}


	private static class DummyMessageReader implements HttpMessageReader<Object> {

		@Override
		public boolean canRead(ResolvableType type, MediaType mediaType) {
			return false;
		}

		@Override
		public List<MediaType> getReadableMediaTypes() {
			return Collections.emptyList();
		}

		@Override
		public Flux<Object> read(ResolvableType type, ReactiveHttpInputMessage inputMessage,
				Map<String, Object> hints) {
			return Flux.empty();
		}

		@Override
		public Mono<Object> readMono(ResolvableType type, ReactiveHttpInputMessage inputMessage,
				Map<String, Object> hints) {
			return Mono.empty();
		}
	}

}
