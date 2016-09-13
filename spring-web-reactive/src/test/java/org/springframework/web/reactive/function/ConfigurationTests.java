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

package org.springframework.web.reactive.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class ConfigurationTests {

	@Test
	public void toConfiguration() throws Exception {
		StaticApplicationContext applicationContext = new StaticApplicationContext();
		applicationContext.registerSingleton("messageWriter", DummyMessageWriter.class);
		applicationContext.registerSingleton("messageReader", DummyMessageReader.class);
		applicationContext.refresh();

		Configuration configuration = Configuration.toConfiguration(applicationContext);
		assertTrue(configuration.messageReaders().get()
				.allMatch(r -> r instanceof DummyMessageReader));
		assertTrue(configuration.messageWriters().get()
				.allMatch(r -> r instanceof DummyMessageWriter));

	}


	private static class DummyMessageWriter implements HttpMessageWriter<Object> {

		@Override
		public boolean canWrite(ResolvableType type, MediaType mediaType, Map<String, Object> hints) {
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
		public boolean canRead(ResolvableType type, MediaType mediaType, Map<String, Object> hints) {
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

