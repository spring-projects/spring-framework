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

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.BodyExtractors;
import org.springframework.util.Assert;
import org.springframework.web.client.reactive.ClientRequest;
import org.springframework.web.client.reactive.WebClient;

/**
 * @author Sebastien Deleuze
 */
public class FlushingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private WebClient webClient;

	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create(new ReactorClientHttpConnector());
	}

	@Test
	public void writeAndFlushWith() throws Exception {
		ClientRequest<Void> request = ClientRequest.GET("http://localhost:" + port + "/write-and-flush").build();
		Mono<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(BodyExtractors.toFlux(String.class)))
				.takeUntil(s -> s.endsWith("data1"))
				.reduce((s1, s2) -> s1 + s2);

		StepVerifier.create(result)
				.expectNext("data0data1")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@Test  // SPR-14991
	public void writeAndAutoFlushOnComplete() {
		ClientRequest<Void> request = ClientRequest.GET("http://localhost:" + port + "/write-and-complete").build();
		Mono<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.bodyToFlux(String.class))
				.reduce((s1, s2) -> s1 + s2);

		StepVerifier.create(result)
				.consumeNextWith(value -> Assert.isTrue(value.length() == 200000))
				.expectComplete()
				.verify();
	}

	@Override
	protected HttpHandler createHttpHandler() {
		return new FlushingHandler();
	}

	private static class FlushingHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			String path = request.getURI().getPath();
			if (path.endsWith("write-and-flush")) {
				Flux<Publisher<DataBuffer>> responseBody = Flux
						.intervalMillis(50)
						.map(l -> toDataBuffer("data" + l, response.bufferFactory()))
						.take(2)
						.map(Flux::just);
				responseBody = responseBody.concatWith(Flux.never());
				return response.writeAndFlushWith(responseBody);
			}
			else if (path.endsWith("write-and-complete")){
				Flux<DataBuffer> responseBody = Flux
						.just("0123456789")
						.repeat(20000)
						.map(value -> toDataBuffer(value, response.bufferFactory()));
				return response.writeWith(responseBody);
			}
			return response.writeWith(Flux.empty());
		}

		private DataBuffer toDataBuffer(String value, DataBufferFactory factory) {
			byte[] data = (value).getBytes(StandardCharsets.UTF_8);
			DataBuffer buffer = factory.allocateBuffer(data.length);
			buffer.write(data);
			return buffer;
		}

	}

}
