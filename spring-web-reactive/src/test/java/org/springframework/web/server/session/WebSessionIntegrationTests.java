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
package org.springframework.web.server.session;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Integration tests for with a server-side session.
 *
 * @author Rossen Stoyanchev
 */
public class WebSessionIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private RestTemplate restTemplate;

	private DefaultWebSessionManager sessionManager;

	private TestWebHandler handler;


	@Override
	public void setup() throws Exception {
		super.setup();
		this.restTemplate = new RestTemplate();
	}

	protected URI createUri(String pathAndQuery) throws URISyntaxException {
		boolean prefix = !StringUtils.hasText(pathAndQuery) || !pathAndQuery.startsWith("/");
		pathAndQuery = (prefix ? "/" + pathAndQuery : pathAndQuery);
		return new URI("http://localhost:" + port + pathAndQuery);
	}

	@Override
	protected HttpHandler createHttpHandler() {
		this.sessionManager = new DefaultWebSessionManager();
		this.handler = new TestWebHandler();
		return WebHttpHandlerBuilder.webHandler(this.handler).sessionManager(this.sessionManager).build();
	}

	@Test
	public void createSession() throws Exception {
		RequestEntity<Void> request = RequestEntity.get(createUri("/")).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);
		assertEquals(1, this.handler.getCount());

		request = RequestEntity.get(createUri("/")).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getHeaders().get("Set-Cookie"));
		assertEquals(2, this.handler.getCount());
	}

	@Test
	public void expiredSession() throws Exception {

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri("/")).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);
		assertEquals(1, this.handler.getCount());

		// Set (server-side) clock back 31 minutes
		Clock clock = this.sessionManager.getClock();
		this.sessionManager.setClock(Clock.offset(clock, Duration.ofMinutes(-31)));

		// Second request: lastAccessTime updated (with offset clock)
		request = RequestEntity.get(createUri("/")).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getHeaders().get("Set-Cookie"));
		assertEquals(2, this.handler.getCount());

		// Third request: new session replaces expired session
		request = RequestEntity.get(createUri("/")).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		id = extractSessionId(response.getHeaders());
		assertNotNull("Expected new session id", id);
		assertEquals("Expected new session attribute", 1, this.handler.getCount());
	}

	private String extractSessionId(HttpHeaders headers) {
		List<String> headerValues = headers.get("Set-Cookie");
		assertNotNull(headerValues);
		assertEquals(1, headerValues.size());

		List<String> data = new ArrayList<>();
		for (String s : headerValues.get(0).split(";")){
			if (s.startsWith("SESSION=")) {
				return s.substring("SESSION=".length());
			}
		}
		return null;
	}

	private static class TestWebHandler implements WebHandler {

		private AtomicInteger currentValue = new AtomicInteger();


		public int getCount() {
			return this.currentValue.get();
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			return exchange.getSession().map(session -> {
				Map<String, Object> map = session.getAttributes();
				int value = (map.get("counter") != null ? (int) map.get("counter") : 0);
				value++;
				map.put("counter", value);
				this.currentValue.set(value);
				return session;
			}).then();
		}
	}

}
