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

import java.net.URI;
import java.util.Random;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

public class RandomHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	public static final int REQUEST_SIZE = 4096 * 3;

	public static final int RESPONSE_SIZE = 1024 * 4;

	private final Random rnd = new Random();

	private final RandomHandler handler = new RandomHandler();

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


	@Override
	protected RandomHandler createHttpHandler() {
		return handler;
	}


	@Test
	public void random() throws Throwable {
		// TODO: fix Reactor support
		assumeFalse(server instanceof ReactorHttpServer);

		RestTemplate restTemplate = new RestTemplate();

		byte[] body = randomBytes();
		RequestEntity<byte[]> request = RequestEntity.post(new URI("http://localhost:" + port)).body(body);
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertNotNull(response.getBody());
		assertEquals(RESPONSE_SIZE,
				response.getHeaders().getContentLength());
		assertEquals(RESPONSE_SIZE, response.getBody().length);

		while (!handler.requestComplete) {
			Thread.sleep(100);
		}
		if (handler.requestError != null) {
			throw handler.requestError;
		}
		assertEquals(REQUEST_SIZE, handler.requestSize);
	}


	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

	private class RandomHandler implements HttpHandler {

		public static final int CHUNKS = 16;

		private volatile boolean requestComplete;

		private int requestSize;

		private Throwable requestError;

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			requestError = null;

			request.getBody().subscribe(new Subscriber<DataBuffer>() {

				@Override
				public void onSubscribe(Subscription s) {
					requestComplete = false;
					requestSize = 0;
					requestError = null;
					s.request(Long.MAX_VALUE);
				}

				@Override
				public void onNext(DataBuffer bytes) {
					requestSize += bytes.readableByteCount();
				}

				@Override
				public void onError(Throwable t) {
					requestComplete = true;
					requestError = t;
				}

				@Override
				public void onComplete() {
					requestComplete = true;
				}
			});

			response.getHeaders().setContentLength(RESPONSE_SIZE);
			return response.setBody(multipleChunks());
		}

		private Publisher<DataBuffer> singleChunk() {
			return Mono.just(randomBuffer(RESPONSE_SIZE));
		}

		private Publisher<DataBuffer> multipleChunks() {
			int chunkSize = RESPONSE_SIZE / CHUNKS;
			return Flux.range(1, CHUNKS).map(integer -> randomBuffer(chunkSize));
		}

		private DataBuffer randomBuffer(int size) {
			byte[] bytes = new byte[size];
			rnd.nextBytes(bytes);
			DataBuffer buffer = dataBufferFactory.allocateBuffer(size);
			buffer.write(bytes);
			return buffer;
		}

	}
}
