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

import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * @author Arjen Poutsma
 */
public class ErrorHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private ErrorHandler handler = new ErrorHandler();

	@Override
	protected HttpHandler createHttpHandler() {
		return handler;
	}

	@Test
	public void response() throws Exception {
		// TODO: fix Reactor
		assumeFalse(server instanceof ReactorHttpServer);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

		ResponseEntity<String> response = restTemplate
				.getForEntity(new URI("http://localhost:" + port + "/response"),
						String.class);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	@Test
	public void returnValue() throws Exception {
		// TODO: fix Reactor
		assumeFalse(server instanceof ReactorHttpServer);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(NO_OP_ERROR_HANDLER);

		ResponseEntity<String> response = restTemplate
				.getForEntity(new URI("http://localhost:" + port + "/returnValue"),
						String.class);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
	}

	private static class ErrorHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Exception error = new UnsupportedOperationException();
			String path = request.getURI().getPath();
			if (path.endsWith("response")) {
				return response.writeWith(Mono.error(error));
			}
			else if (path.endsWith("returnValue")) {
				return Mono.error(error);
			}
			else {
				return Mono.empty();
			}
		}
	}

	private static final ResponseErrorHandler NO_OP_ERROR_HANDLER =
			new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return false;
				}

				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
				}
			};

}
