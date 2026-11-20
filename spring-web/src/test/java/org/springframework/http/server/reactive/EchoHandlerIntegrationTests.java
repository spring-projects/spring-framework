/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.util.Random;

import reactor.core.publisher.Mono;

import org.springframework.http.ResponseEntity;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class EchoHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final int REQUEST_SIZE = 4096 * 3;

	private final Random rnd = new Random();


	@Override
	protected EchoHandler createHttpHandler() {
		return new EchoHandler();
	}


	@ParameterizedHttpServerTest
	public void echo(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		byte[] body = randomBytes();
		ResponseEntity<byte[]> response = getRestClient().post().body(body).retrieve().toEntity(byte[].class);

		assertThat(response.getBody()).isEqualTo(body);
	}


	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

	/**
	 * @author Arjen Poutsma
	 */
	public static class EchoHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			return response.writeWith(request.getBody());
		}
	}

}
