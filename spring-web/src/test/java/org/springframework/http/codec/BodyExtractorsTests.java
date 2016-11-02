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

package org.springframework.http.codec;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * @author Arjen Poutsma
 */
public class BodyExtractorsTests {

	private BodyExtractor.Context context;

	@Before
	public void createContext() {
		final List<HttpMessageReader<?>> messageReaders = new ArrayList<>();
		messageReaders.add(new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		messageReaders.add(new DecoderHttpMessageReader<>(new StringDecoder()));
		messageReaders.add(new DecoderHttpMessageReader<>(new Jaxb2XmlDecoder()));
		messageReaders.add(new DecoderHttpMessageReader<>(new Jackson2JsonDecoder()));

		this.context = new BodyExtractor.Context() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return messageReaders::stream;
			}
		};

	}

	@Test
	public void toMono() throws Exception {
		BodyExtractor<Mono<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toMono(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.setBody(body);

		Mono<String> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.expectNext("foo")
				.expectComplete()
				.verify();
	}

	@Test
	public void toFlux() throws Exception {
		BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.setBody(body);

		Flux<String> result = extractor.extract(request, this.context);

		StepVerifier.create(result)
				.expectNext("foo")
				.expectComplete()
				.verify();
	}

	@Test
	public void toFluxUnacceptable() throws Exception {
		BodyExtractor<Flux<String>, ReactiveHttpInputMessage> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		request.setBody(body);

		BodyExtractor.Context emptyContext = new BodyExtractor.Context() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return () -> Collections.<HttpMessageReader<?>>emptySet().stream();
			}
		};

		Flux<String> result = extractor.extract(request, emptyContext);
		StepVerifier.create(result)
				.expectError(UnsupportedMediaTypeException.class)
				.verify();
	}

}