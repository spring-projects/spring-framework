/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.server.session;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DefaultWebSessionManager} with a server-side session.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class WebSessionIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();

	private final DefaultWebSessionManager sessionManager = new DefaultWebSessionManager();

	private final TestWebHandler handler = new TestWebHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return WebHttpHandlerBuilder.webHandler(this.handler).sessionManager(this.sessionManager).build();
	}


	@ParameterizedHttpServerTest
	public void createSession(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String id = extractSessionId(response.getHeaders());
		assertThat(id).isNotNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(1);

		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().get("Set-Cookie")).isNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void expiredSessionIsRecreated(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String id = extractSessionId(response.getHeaders());
		assertThat(id).isNotNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(1);

		// Second request: same session
		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().get("Set-Cookie")).isNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(2);

		// Now fast-forward by 31 minutes
		InMemoryWebSessionStore store = (InMemoryWebSessionStore) this.sessionManager.getSessionStore();
		WebSession session = store.retrieveSession(id).block();
		assertThat(session).isNotNull();
		store.setClock(Clock.offset(store.getClock(), Duration.ofMinutes(31)));

		// Third request: expired session, new session created
		request = RequestEntity.get(createUri()).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		id = extractSessionId(response.getHeaders());
		assertThat(id).as("Expected new session id").isNotNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(1);
	}

	@ParameterizedHttpServerTest
	public void expiredSessionEnds(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String id = extractSessionId(response.getHeaders());
		assertThat(id).isNotNull();

		// Now fast-forward by 31 minutes
		InMemoryWebSessionStore store = (InMemoryWebSessionStore) this.sessionManager.getSessionStore();
		store.setClock(Clock.offset(store.getClock(), Duration.ofMinutes(31)));

		// Second request: session expires
		URI uri = new URI("http://localhost:" + this.port + "/?expire");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String value = response.getHeaders().getFirst("Set-Cookie");
		assertThat(value).isNotNull();
		assertThat(value.contains("Max-Age=0")).as("Actual value: " + value).isTrue();
	}

	@ParameterizedHttpServerTest
	public void changeSessionId(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String oldId = extractSessionId(response.getHeaders());
		assertThat(oldId).isNotNull();
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(1);

		// Second request: session id changes
		URI uri = new URI("http://localhost:" + this.port + "/?changeId");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + oldId).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String newId = extractSessionId(response.getHeaders());
		assertThat(newId).as("Expected new session id").isNotNull();
		assertThat(newId).isNotEqualTo(oldId);
		assertThat(this.handler.getSessionRequestCount()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void invalidate(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		// First request: no session yet, new session created
		RequestEntity<Void> request = RequestEntity.get(createUri()).build();
		ResponseEntity<Void> response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String id = extractSessionId(response.getHeaders());
		assertThat(id).isNotNull();

		// Second request: invalidates session
		URI uri = new URI("http://localhost:" + this.port + "/?invalidate");
		request = RequestEntity.get(uri).header("Cookie", "SESSION=" + id).build();
		response = this.restTemplate.exchange(request, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String value = response.getHeaders().getFirst("Set-Cookie");
		assertThat(value).isNotNull();
		assertThat(value.contains("Max-Age=0")).as("Actual value: " + value).isTrue();
	}

	private String extractSessionId(HttpHeaders headers) {
		List<String> headerValues = headers.get("Set-Cookie");
		assertThat(headerValues).isNotNull();
		assertThat(headerValues.size()).isEqualTo(1);

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
