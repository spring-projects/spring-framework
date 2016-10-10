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

import static org.junit.Assert.*;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ServerHttpRequestIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected CheckRequestHandler createHttpHandler() {
		return new CheckRequestHandler();
	}

	@Test
	public void checkUri() throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		RequestEntity<Void> request = RequestEntity.post(new URI("http://localhost:" + port + "/foo?param=bar")).build();
		ResponseEntity<Void> response = restTemplate.exchange(request, Void.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}


	public static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertNotNull("Request URI host must not be null", uri.getHost());
			assertNotEquals("Request URI port must not be undefined", -1, uri.getPort());
			assertEquals("Request URI path is not valid", "/foo", uri.getPath());
			assertEquals("Request URI query is not valid", "param=bar", uri.getQuery());
			return Mono.empty();
		}
	}
}
