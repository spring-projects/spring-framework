/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.net.URI;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class ServerHttpRequestIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected CheckRequestHandler createHttpHandler() {
		return new CheckRequestHandler();
	}


	@ParameterizedHttpServerTest
	void checkUri(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> response = getRestClient().post().uri("/foo?param=bar").retrieve().toBodilessEntity();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}


	static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertThat(uri.getScheme()).isEqualTo("http");
			assertThat(uri.getHost()).isNotNull();
			assertThat(uri.getPort()).isNotEqualTo(-1);
			assertThat(request.getRemoteAddress()).isNotNull();
			assertThat(uri.getPath()).isEqualTo("/foo");
			assertThat(uri.getQuery()).isEqualTo("param=bar");
			return Mono.empty();
		}
	}

}
