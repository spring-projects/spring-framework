/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;

/**
 * @author Sebastien Deleuze
 */
public class ServerHttpRequestIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected CheckRequestHandler createHttpHandler() {
		return new CheckRequestHandler();
	}

	@Test
	public void checkUri() throws Exception {
		URI url = new URI("http://localhost:" + port + "/foo?param=bar");
		RequestEntity<Void> request = RequestEntity.post(url).build();
		ResponseEntity<Void> response = new RestTemplate().exchange(request, Void.class);
		assertEquals(HttpStatus.OK, response.getStatusCode());
	}


	public static class CheckRequestHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			URI uri = request.getURI();
			assertEquals("http", uri.getScheme());
			assertNotNull(uri.getHost());
			assertNotEquals(-1, uri.getPort());
			assertNotNull(request.getRemoteAddress());
			assertEquals("/foo", uri.getPath());
			assertEquals("param=bar", uri.getQuery());
			return Mono.empty();
		}
	}

}
