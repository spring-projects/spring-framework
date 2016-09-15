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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

/**
 * @author Arjen Poutsma
 */
public class BodyExtractorsTests {

	@Test
	public void toMono() throws Exception {
		BodyExtractor<Mono<String>> extractor = BodyExtractors.toMono(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.setBody(body);

		Configuration configuration = Configuration.builder().build();

		Mono<String> result = extractor.extract(request, configuration);

		TestSubscriber.subscribe(result)
				.assertComplete()
				.assertValues("foo");
	}

	@Test
	public void toFlux() throws Exception {
		BodyExtractor<Flux<String>> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.setBody(body);

		Configuration configuration = Configuration.builder().build();

		Flux<String> result = extractor.extract(request, configuration);
		TestSubscriber.subscribe(result)
				.assertComplete()
				.assertValues("foo");
	}

	@Test
	public void toFluxUnacceptable() throws Exception {
		BodyExtractor<Flux<String>> extractor = BodyExtractors.toFlux(String.class);

		DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
		DefaultDataBuffer dataBuffer =
				factory.wrap(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));
		Flux<DataBuffer> body = Flux.just(dataBuffer);

		MockServerHttpRequest request = new MockServerHttpRequest();
		request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		request.setBody(body);

		Configuration configuration = Configuration.empty().build();

		Flux<String> result = extractor.extract(request, configuration);
		TestSubscriber.subscribe(result)
				.assertError(UnsupportedMediaTypeStatusException.class);

	}

}