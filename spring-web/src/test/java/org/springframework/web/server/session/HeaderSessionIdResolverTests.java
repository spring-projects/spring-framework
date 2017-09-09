/*
 * Copyright 2017 the original author or authors.
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

import static org.hamcrest.collection.IsCollectionWithSize.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsCollectionContaining.*;
import static org.junit.Assert.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

/**
 * Tests using {@link HeaderSessionIdResolver}.
 *
 * @author Greg Turnquist
 */
public class HeaderSessionIdResolverTests {

	private static final Clock CLOCK = Clock.system(ZoneId.of("GMT"));

	private HeaderSessionIdResolver idResolver;

	private DefaultWebSessionManager manager;

	private ServerWebExchange exchange;

	@Before
	public void setUp() {
		this.idResolver = new HeaderSessionIdResolver();
		this.manager = new DefaultWebSessionManager();
		this.manager.setSessionIdResolver(this.idResolver);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();

		this.exchange = new DefaultServerWebExchange(request, response, this.manager,
			ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}

	@Test
	public void getSessionWithoutStarting() throws Exception {
		WebSession session = this.manager.getSession(this.exchange).block();
		session.save();

		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		assertNull(this.manager.getSessionStore().retrieveSession(session.getId()).block());
	}

	@Test
	public void startSessionExplicitly() throws Exception {
		WebSession session = this.manager.getSession(this.exchange).block();
		session.start();
		session.save().block();

		assertThat(this.exchange.getResponse().getHeaders().containsKey("SESSION"), is(true));
		assertThat(this.exchange.getResponse().getHeaders().get("SESSION"), hasSize(1));
		assertThat(this.exchange.getResponse().getHeaders().get("SESSION"), hasItem(session.getId()));
	}

	@Test
	public void startSessionImplicitly() throws Exception {
		WebSession session = this.manager.getSession(this.exchange).block();
		session.getAttributes().put("foo", "bar");
		session.save();

		assertNotNull(this.exchange.getResponse().getHeaders().get("SESSION"));
	}

	@Test
	public void existingSession() throws Exception {
		UUID sessionId = UUID.randomUUID();
		DefaultWebSession existing = createDefaultWebSession(sessionId);
		this.manager.getSessionStore().storeSession(existing);

		this.exchange = this.exchange.mutate()
			.request(this.exchange.getRequest().mutate()
				.header("SESSION", sessionId.toString())
				.build())
			.build();

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}

	@Test
	public void existingSessionIsExpired() throws Exception {
		UUID sessionId = UUID.randomUUID();
		DefaultWebSession existing = createDefaultWebSession(sessionId);
		existing.start();
		Instant lastAccessTime = Instant.now(CLOCK).minus(Duration.ofMinutes(31));
		existing = new DefaultWebSession(existing, lastAccessTime, s -> Mono.empty());
		this.manager.getSessionStore().storeSession(existing);

		this.exchange = this.exchange.mutate()
			.request(this.exchange.getRequest().mutate()
				.header("SESSION", sessionId.toString())
				.build())
			.build();

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotSame(existing, actual);
	}

	@Test
	public void multipleSessionIds() throws Exception {
		UUID sessionId = UUID.randomUUID();
		DefaultWebSession existing = createDefaultWebSession(sessionId);
		this.manager.getSessionStore().storeSession(existing);
		this.manager.getSessionStore().storeSession(createDefaultWebSession(UUID.randomUUID()));
		this.manager.getSessionStore().storeSession(createDefaultWebSession(UUID.randomUUID()));

		this.exchange = this.exchange.mutate()
			.request(this.exchange.getRequest().mutate()
				.header("SESSION", sessionId.toString())
				.build())
			.build();

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}

	@Test
	public void alternateHeaderName() throws Exception {
		this.idResolver.setHeaderName("alternateHeaderName");

		UUID sessionId = UUID.randomUUID();
		DefaultWebSession existing = createDefaultWebSession(sessionId);
		this.manager.getSessionStore().storeSession(existing);

		this.exchange = this.exchange.mutate()
			.request(this.exchange.getRequest().mutate()
				.header("alternateHeaderName", sessionId.toString())
				.build())
			.build();

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}

	private DefaultWebSession createDefaultWebSession(UUID sessionId) {
		return new DefaultWebSession(() -> sessionId, CLOCK, (s, session) -> Mono.empty(), s -> Mono.empty());
	}
}
