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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
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

	private final DefaultWebSessionManager manager = new DefaultWebSessionManager();

	private final TestWebSessionIdResolver idResolver = new TestWebSessionIdResolver();

	private DefaultServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.manager.setSessionIdResolver(this.idResolver);

		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/path"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.manager);
	}


	@Test
	public void getSessionWithoutStarting() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).get();
		session.save();

		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		assertNull(this.idResolver.getSavedId());
		assertNull(this.manager.getSessionStore().retrieveSession(session.getId()).get());
	}

	@Test
	public void startSessionExplicitly() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).get();
		session.start();
		session.save();

		String id = session.getId();
		assertNotNull(this.idResolver.getSavedId());
		assertEquals(id, this.idResolver.getSavedId());
		assertSame(session, this.manager.getSessionStore().retrieveSession(id).get());
	}

	@Test
	public void startSessionImplicitly() throws Exception {
		this.idResolver.setIdsToResolve(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).get();
		session.getAttributes().put("foo", "bar");
		session.save();

		assertNotNull(this.idResolver.getSavedId());
	}

	@Test
	public void existingSession() throws Exception {
		DefaultWebSession existing = new DefaultWebSession("1", Clock.systemDefaultZone());
		this.manager.getSessionStore().storeSession(existing);
		this.idResolver.setIdsToResolve(Collections.singletonList("1"));

		WebSession actual = this.manager.getSession(this.exchange).get();
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

		WebSession actual = this.manager.getSession(this.exchange).get();
		assertNotSame(existing, actual);
	}

	@Test
	public void multipleSessions() throws Exception {
		DefaultWebSession existing = new DefaultWebSession("3", Clock.systemDefaultZone());
		this.manager.getSessionStore().storeSession(existing);
		this.idResolver.setIdsToResolve(Arrays.asList("1", "2", "3"));

		WebSession actual = this.manager.getSession(this.exchange).get();
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
