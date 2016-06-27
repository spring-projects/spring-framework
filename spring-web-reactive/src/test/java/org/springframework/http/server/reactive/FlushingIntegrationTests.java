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

package org.springframework.http.server.reactive;

import org.junit.Before;
import org.junit.Test;
import static org.springframework.web.client.reactive.HttpRequestBuilders.get;
import static org.springframework.web.client.reactive.WebResponseExtractors.bodyStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.test.TestSubscriber;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.http.client.reactive.ReactorHttpClientRequestFactory;
import org.springframework.web.client.reactive.WebClient;

/**
 * @author Sebastien Deleuze
 */
public class FlushingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private WebClient webClient;

	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = new WebClient(new ReactorHttpClientRequestFactory());
	}

	@Test
	public void testFlushing() throws Exception {
		Mono<String> result = this.webClient
				.perform(get("http://localhost:" + port))
				.extract(bodyStream(String.class))
				.takeUntil(s -> {
					return s.endsWith("data1");
				})
				.reduce((s1, s2) -> s1 + s2);

		TestSubscriber
				.subscribe(result)
				.await()
				.assertValues("data0data1");
	}


	@Override
	protected HttpHandler createHttpHandler() {
		return new FlushingHandler();
	}

	// Handler that never completes designed to test if flushing is perform correctly when
	// a FlushingDataBuffer is written
	private static class FlushingHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Flux<DataBuffer> responseBody = Flux
					.interval(50)
					.map(l -> {
						byte[] data = ("data" + l).getBytes();
						DataBuffer buffer = response.bufferFactory().allocateBuffer(data.length);
						buffer.write(data);
						return buffer;
					})
					.take(2)
					.concatWith(Mono.just(FlushingDataBuffer.INSTANCE))
					.concatWith(Flux.never());
			return response.writeWith(responseBody);
		}
	}
}
