/*
 * Copyright 2002-2020 the original author or authors.
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
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
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

		URI url = new URI("http://localhost:" + port + "/foo?param=bar");
		RequestEntity<Void> request = RequestEntity.post(url).build();
		@SuppressWarnings("resource")
		ResponseEntity<Void> response = new RestTemplate().exchange(request, Void.class);
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
