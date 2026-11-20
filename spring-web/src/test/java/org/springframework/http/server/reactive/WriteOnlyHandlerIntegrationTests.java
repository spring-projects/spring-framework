/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
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

		this.body = randomBytes();
		ResponseEntity<byte[]> response = getRestClient().post().body("".getBytes(StandardCharsets.UTF_8))
				.retrieve().toEntity(byte[].class);

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
