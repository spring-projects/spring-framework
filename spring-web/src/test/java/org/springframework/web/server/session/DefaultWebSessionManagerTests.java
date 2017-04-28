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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Rossen Stoyanchev
 */
public class DefaultWebSessionManagerTests {

	private DefaultWebSessionManager manager;

	private TestWebSessionIdResolver idResolver;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.manager = new DefaultWebSessionManager();
		this.idResolver = new TestWebSessionIdResolver();
		this.manager.setSessionIdResolver(this.idResolver);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.manager, ServerCodecConfigurer.create());
	}


	@Test
	public void getSessionWithoutStarting() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.save();

		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		assertNull(this.idResolver.getSavedId());
		assertNull(this.manager.getSessionStore().retrieveSession(session.getId()).block());
	}

	@Test
	public void startSessionExplicitly() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.start();
		session.save();

		String id = session.getId();
		assertNotNull(this.idResolver.getSavedId());
		assertEquals(id, this.idResolver.getSavedId());
		assertSame(session, this.manager.getSessionStore().retrieveSession(id).block());
	}

	@Test
	public void startSessionImplicitly() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.getAttributes().put("foo", "bar");
		session.save();

		assertNotNull(this.idResolver.getSavedId());
	}

	@Test
	public void existingSession() throws Exception {
		DefaultWebSession existing = new DefaultWebSession("1", Clock.systemDefaultZone());
		this.manager.getSessionStore().storeSession(existing);
		this.idResolver.setIdsToResolve(Collections.singletonList("1"));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertSame(existing, actual);
	}

	@Test
	public void existingSessionIsExpired() throws Exception {
		Clock clock = Clock.systemDefaultZone();
		DefaultWebSession existing = new DefaultWebSession("1", clock);
		existing.start();
		existing.setLastAccessTime(Instant.now(clock).minus(Duration.ofMinutes(31)));
		this.manager.getSessionStore().storeSession(existing);
		this.idResolver.setIdsToResolve(Collections.singletonList("1"));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotSame(existing, actual);
	}

	@Test
	public void multipleSessions() throws Exception {
		DefaultWebSession existing = new DefaultWebSession("3", Clock.systemDefaultZone());
		this.manager.getSessionStore().storeSession(existing);
		this.idResolver.setIdsToResolve(Arrays.asList("1", "2", "3"));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertSame(existing, actual);
	}


	private static class TestWebSessionIdResolver implements WebSessionIdResolver {

		private List<String> idsToResolve = new ArrayList<>();

		private String id = null;


		public void setIdsToResolve(List<String> idsToResolve) {
			this.idsToResolve = idsToResolve;
		}

		public String getSavedId() {
			return this.id;
		}

		@Override
		public List<String> resolveSessionIds(ServerWebExchange exchange) {
			return this.idsToResolve;
		}

		@Override
		public void setSessionId(ServerWebExchange exchange, String sessionId) {
			this.id = sessionId;
		}
	}

}
