/*
 * Copyright 2002-2022 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Random;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Violeta Georgieva
 * @since 5.0
 */
class WriteOnlyHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final int REQUEST_SIZE = 4096 * 3;

	private final Random rnd = new Random();

	private byte[] body;


	@Override
	protected WriteOnlyHandler createHttpHandler() {
		return new WriteOnlyHandler();
	}

	@ParameterizedHttpServerTest
	void writeOnly(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		@SuppressWarnings("resource")
		RestTemplate restTemplate = new RestTemplate();

		this.body = randomBytes();
		RequestEntity<byte[]> request = RequestEntity.post(
				new URI("http://localhost:" + port)).body(
						"".getBytes(StandardCharsets.UTF_8));
		ResponseEntity<byte[]> response = restTemplate.exchange(request, byte[].class);

		assertThat(response.getBody()).isEqualTo(body);
	}

	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}


	class WriteOnlyHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			DataBuffer buffer = response.bufferFactory().allocateBuffer(body.length);
			buffer.write(body);
			return response.writeAndFlushWith(Flux.just(Flux.just(buffer)));
		}
	}

}
