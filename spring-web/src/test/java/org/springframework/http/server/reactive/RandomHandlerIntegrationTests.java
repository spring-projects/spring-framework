/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.util.Random;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RandomHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final int REQUEST_SIZE = 4096 * 3;

	private static final int RESPONSE_SIZE = 1024 * 4;

	private final Random rnd = new Random();

	private final RandomHandler handler = new RandomHandler();

	private final DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();


	@Override
	protected RandomHandler createHttpHandler() {
		return handler;
	}


	@ParameterizedHttpServerTest
	void random(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		// TODO: fix Reactor support

		RestTemplate restTemplate = new RestTemplate();

		byte[] body = randomBytes();
		RequestEntity<byte[]> request = RequestEntity.post(new URI("http://localhost:" + port)).body(body);
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getHeaders().getContentLength()).isEqualTo(RESPONSE_SIZE);
		assertThat(response.getBody().length).isEqualTo(RESPONSE_SIZE);
	}


	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

	private class RandomHandler implements HttpHandler {

		static final int CHUNKS = 16;

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Mono<Integer> requestSizeMono = request.getBody().
					reduce(0, (integer, dataBuffer) -> integer +
							dataBuffer.readableByteCount()).
					doOnNext(size -> assertThat(size).isEqualTo(REQUEST_SIZE)).
					doOnError(throwable -> assertThat(throwable).isNull());

			response.getHeaders().setContentLength(RESPONSE_SIZE);

			return requestSizeMono.then(response.writeWith(multipleChunks()));
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
