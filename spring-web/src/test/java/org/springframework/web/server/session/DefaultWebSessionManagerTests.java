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
package org.springframework.web.server.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWebSessionManager}.
 * @author Rossen Stoyanchev
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultWebSessionManagerTests {

	private static final Clock CLOCK = Clock.system(ZoneId.of("GMT"));

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private DefaultWebSessionManager manager;

	private ServerWebExchange exchange;

	@Mock
	private WebSessionIdResolver idResolver;

	@Mock
	private WebSessionStore store;

	@Before
	public void setUp() throws Exception {
		when(this.store.createWebSession()).thenReturn(Mono.just(createDefaultWebSession()));
		when(this.store.updateLastAccessTime(any())).thenAnswer( invocation -> Mono.just(invocation.getArgument(0)));

		this.manager = new DefaultWebSessionManager();
		this.manager.setSessionIdResolver(this.idResolver);
		this.manager.setSessionStore(this.store);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.manager,
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}


	@Test
	public void getSessionWithoutStarting() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.save().block();

		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		verify(this.store, never()).storeSession(any());
		verify(this.idResolver, never()).setSessionId(any(), any());
	}

	@Test
	public void startSessionExplicitly() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		when(this.store.storeSession(any())).thenReturn(Mono.empty());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.start();
		session.save().block();

		String id = session.getId();
		verify(this.store).createWebSession();
		verify(this.store).storeSession(any());
		verify(this.idResolver).setSessionId(any(), eq(id));
	}

	@Test
	public void startSessionImplicitly() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		when(this.store.storeSession(any())).thenReturn(Mono.empty());
		WebSession session = this.manager.getSession(this.exchange).block();
		session.getAttributes().put("foo", "bar");
		session.save().block();

		verify(this.store).createWebSession();
		verify(this.idResolver).setSessionId(any(), any());
		verify(this.store).storeSession(any());
	}

	@Test
	public void exchangeWhenResponseSetCompleteThenSavesAndSetsId() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		when(this.store.storeSession(any())).thenReturn(Mono.empty());
		WebSession session = this.manager.getSession(this.exchange).block();
		String id = session.getId();
		session.getAttributes().put("foo", "bar");
		this.exchange.getResponse().setComplete().block();

		verify(this.idResolver).setSessionId(any(), eq(id));
		verify(this.store).storeSession(any());
	}

	@Test
	public void existingSession() throws Exception {
		DefaultWebSession existing = createDefaultWebSession();
		String id = existing.getId();
		when(this.store.retrieveSession(id)).thenReturn(Mono.just(existing));
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.singletonList(id));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}

	@Test
	public void existingSessionIsExpired() throws Exception {
		DefaultWebSession existing = createDefaultWebSession();
		existing.start();
		Instant lastAccessTime = Instant.now(CLOCK).minus(Duration.ofMinutes(31));
		existing = new DefaultWebSession(existing, lastAccessTime, s -> Mono.empty());
		when(this.store.retrieveSession(existing.getId())).thenReturn(Mono.just(existing));
		when(this.store.removeSession(existing.getId())).thenReturn(Mono.empty());
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.singletonList(existing.getId()));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotSame(existing, actual);
		verify(this.store).removeSession(existing.getId());
		verify(this.idResolver).expireSession(any());
	}

	@Test
	public void multipleSessionIds() throws Exception {
		DefaultWebSession existing = createDefaultWebSession();
		String id = existing.getId();
		when(this.store.retrieveSession(any())).thenReturn(Mono.empty());
		when(this.store.retrieveSession(id)).thenReturn(Mono.just(existing));
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Arrays.asList("neither-this", "nor-that", id));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}

	private DefaultWebSession createDefaultWebSession() {
		return new DefaultWebSession(idGenerator, CLOCK, (s, session) -> Mono.empty(), s -> Mono.empty());
	}
}
