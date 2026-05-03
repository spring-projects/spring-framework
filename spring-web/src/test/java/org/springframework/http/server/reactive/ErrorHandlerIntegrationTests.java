/*
 * Copyright 2002-present the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyCoreHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyHttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ErrorHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final ErrorHandler handler = new ErrorHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return handler;
	}

	@Override
	protected RestClient initRestClient(RestClient.Builder builder) {
		return builder
				.defaultStatusHandler(HttpStatusCode::is5xxServerError, (req, res) -> {})
				.defaultStatusHandler(HttpStatusCode::is4xxClientError, (req, res) -> {})
				.build();
	}

	@ParameterizedHttpServerTest
	void responseBodyError(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> response = getRestClient().get().uri("/response-body-error")
				.retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ParameterizedHttpServerTest
	void handlingError(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> response = getRestClient().get().uri("/handling-error")
				.retrieve().toEntity(String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ParameterizedHttpServerTest  // SPR-15560
	void emptyPathSegments(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		ResponseEntity<String> response = getRestClient().get()
				.uri(URI.create("http://localhost:" + this.server.getPort() + "//"))
				.retrieve().toEntity(String.class);

		// Jetty 10+ rejects empty path segments, see https://github.com/eclipse/jetty.project/issues/6302,
		// but an application can apply CompactPathRule via RewriteHandler:
		// https://www.eclipse.org/jetty/documentation/jetty-11/programming_guide.php

		HttpStatus expectedStatus = (httpServer instanceof JettyHttpServer || httpServer instanceof JettyCoreHttpServer ?
				HttpStatus.BAD_REQUEST : HttpStatus.OK);

		assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
	}


	private static class ErrorHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Exception error = new UnsupportedOperationException();
			String path = request.getURI().getPath();
			if (path.endsWith("response-body-error")) {
				return response.writeWith(Mono.error(error));
			}
			else if (path.endsWith("handling-error")) {
				return Mono.error(error);
			}
			else {
				return Mono.empty();
			}
		}
	}

}
