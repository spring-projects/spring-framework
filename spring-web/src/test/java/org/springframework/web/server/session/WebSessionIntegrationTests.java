/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.*;

/**
 * Integration tests for with a server-side session.
 *
 * @author Rossen Stoyanchev
 */
public class WebSessionIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();

	private DefaultWebSessionManager sessionManager;

	private TestWebHandler handler;


	@Override
	protected HttpHandler createHttpHandler() {
		this.sessionManager = new DefaultWebSessionManager();
		this.handler = new TestWebHandler();
		return WebHttpHandlerBuilder.webHandler(this.handler).sessionManager(this.sessionManager).build();
	}


	@Test
	public void createSession() throws Exception {
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);
		assertEquals(1, this.handler.getSessionRequestCount());

		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getHeaders().get("Set-Cookie"));
		assertEquals(2, this.handler.getSessionRequestCount());
	}

	@Test
	public void expiredSessionIsRecreated() throws Exception {

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);
		assertEquals(1, this.handler.getSessionRequestCount());

		// Second request: same session
		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNull(response.getHeaders().get("Set-Cookie"));
		assertEquals(2, this.handler.getSessionRequestCount());

		// Now fast-forward by 31 minutes
		InMemoryWebSessionStore store = (InMemoryWebSessionStore) this.sessionManager.getSessionStore();
		WebSession session = store.retrieveSession(id).block();
		assertNotNull(session);
		store.setClock(Clock.offset(store.getClock(), Duration.ofMinutes(31)));

		// Third request: expired session, new session created
		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		id = extractSessionId(response.getHeaders());
		assertNotNull("Expected new session id", id);
		assertEquals(1, this.handler.getSessionRequestCount());
	}

	@Test
	public void expiredSessionEnds() throws Exception {

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);

		// Now fast-forward by 31 minutes
		InMemoryWebSessionStore store = (InMemoryWebSessionStore) this.sessionManager.getSessionStore();
		store.setClock(Clock.offset(store.getClock(), Duration.ofMinutes(31)));

		// Second request: session expires
		URI uri = new URI("http://localhost:" + this.port + "/?expire");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String value = response.getHeaders().getFirst("Set-Cookie");
		assertNotNull(value);
		assertTrue("Actual value: " + value, value.contains("Max-Age=0"));
	}

	@Test
	public void changeSessionId() throws Exception {

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String oldId = extractSessionId(response.getHeaders());
		assertNotNull(oldId);
		assertEquals(1, this.handler.getSessionRequestCount());

		// Second request: session id changes
		URI uri = new URI("http://localhost:" + this.port + "/?changeId");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + oldId).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String newId = extractSessionId(response.getHeaders());
		assertNotNull("Expected new session id", newId);
		assertNotEquals(oldId, newId);
		assertEquals(2, this.handler.getSessionRequestCount());
	}

	@Test
	public void invalidate() throws Exception {

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String id = extractSessionId(response.getHeaders());
		assertNotNull(id);

		// Second request: invalidates session
		URI uri = new URI("http://localhost:" + this.port + "/?invalidate");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		String value = response.getHeaders().getFirst("Set-Cookie");
		assertNotNull(value);
		assertTrue("Actual value: " + value, value.contains("Max-Age=0"));
	}

	private String extractSessionId(HttpHeaders headers) {
		List<String> headerValues = headers.get("Set-Cookie");
		assertNotNull(headerValues);
		assertEquals(1, headerValues.size());

		for (String s : headerValues.get(0).split(";")){
			if (s.startsWith("SESSION=")) {
				return s.substring("SESSION=".length());
			}
		}
		return null;
	}

	private URI createUri() throws URISyntaxException {
		return new URI("http://localhost:" + this.port + "/");
	}


	private static class TestWebHandler implements WebHandler {

		private AtomicInteger currentValue = new AtomicInteger();


		public int getSessionRequestCount() {
			return this.currentValue.get();
		}

		@Override
		public Mono<Void> handle(ServerWebExchange exchange) {
			if (exchange.getRequest().getQueryParams().containsKey("expire")) {
				return exchange.getSession().doOnNext(session -> {
					// Don't do anything, leave it expired...
				}).then();
			}
			else if (exchange.getRequest().getQueryParams().containsKey("changeId")) {
				return exchange.getSession().flatMap(session ->
						session.changeSessionId().doOnSuccess(aVoid -> updateSessionAttribute(session)));
			}
			else if (exchange.getRequest().getQueryParams().containsKey("invalidate")) {
				return exchange.getSession().doOnNext(WebSession::invalidate).then();
			}
			else {
				return exchange.getSession().doOnSuccess(this::updateSessionAttribute).then();
			}
		}

		private void updateSessionAttribute(WebSession session) {
			int value = session.getAttributeOrDefault("counter", 0);
			session.getAttributes().put("counter", ++value);
			this.currentValue.set(value);
		}
	}

}
