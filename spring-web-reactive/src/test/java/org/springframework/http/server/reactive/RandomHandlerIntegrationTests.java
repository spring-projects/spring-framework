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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;

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

		RestTemplate restTemplate = new RestTemplate();

		byte[] body = randomBytes();
		RequestEntity<byte[]> request = RequestEntity.post(new URI("http://localhost:" + port)).body(body);
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertNotNull(response.getBody());
		assertEquals(RESPONSE_SIZE,
				response.getHeaders().getContentLength());
		assertEquals(RESPONSE_SIZE, response.getBody().length);
	}


	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

	private class RandomHandler implements HttpHandler {

		public static final int CHUNKS = 16;

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Mono<Integer> requestSizeMono = request.getBody().
					reduce(0, (integer, dataBuffer) -> integer +
							dataBuffer.readableByteCount()).
					doAfterTerminate((size, throwable) -> {
						assertNull(throwable);
						assertEquals(REQUEST_SIZE, (long) size);
					});



			response.getHeaders().setContentLength(RESPONSE_SIZE);

			return requestSizeMono.then(response.writeWith(multipleChunks()));
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
