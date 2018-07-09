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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWebSessionManager}.
 * @author Rossen Stoyanchev
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultWebSessionManagerTests {

	private DefaultWebSessionManager sessionManager;

	private ServerWebExchange exchange;

	@Mock
	private WebSessionIdResolver sessionIdResolver;

	@Mock
	private WebSessionStore sessionStore;

	@Mock
	private WebSession createSession;

	@Mock
	private WebSession updateSession;


	@Before
	public void setUp() throws Exception {

		when(this.createSession.save()).thenReturn(Mono.empty());
		when(this.createSession.getId()).thenReturn("create-session-id");
		when(this.updateSession.getId()).thenReturn("update-session-id");

		when(this.sessionStore.createWebSession()).thenReturn(Mono.just(this.createSession));
		when(this.sessionStore.retrieveSession(this.updateSession.getId())).thenReturn(Mono.just(this.updateSession));

		this.sessionManager = new DefaultWebSessionManager();
		this.sessionManager.setSessionIdResolver(this.sessionIdResolver);
		this.sessionManager.setSessionStore(this.sessionStore);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.sessionManager,
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}

	@Test
	public void getSessionSaveWhenCreatedAndNotStartedThenNotSaved() {

		when(this.sessionIdResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		WebSession session = this.sessionManager.getSession(this.exchange).block();
		this.exchange.getResponse().setComplete().block();

		assertSame(this.createSession, session);
		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		verify(this.createSession, never()).save();
		verify(this.sessionIdResolver, never()).setSessionId(any(), any());
	}

	@Test
	public void getSessionSaveWhenCreatedAndStartedThenSavesAndSetsId() {

		when(this.sessionIdResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		WebSession session = this.sessionManager.getSession(this.exchange).block();
		assertSame(this.createSession, session);
		String sessionId = this.createSession.getId();

		when(this.createSession.isStarted()).thenReturn(true);
		this.exchange.getResponse().setComplete().block();

		verify(this.sessionStore).createWebSession();
		verify(this.sessionIdResolver).setSessionId(any(), eq(sessionId));
		verify(this.createSession).save();
	}

	@Test
	public void existingSession() {

		String sessionId = this.updateSession.getId();
		when(this.sessionIdResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.singletonList(sessionId));

		WebSession actual = this.sessionManager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(sessionId, actual.getId());
	}

	@Test
	public void multipleSessionIds() {

		List<String> ids = Arrays.asList("not-this", "not-that", this.updateSession.getId());
		when(this.sessionStore.retrieveSession("not-this")).thenReturn(Mono.empty());
		when(this.sessionStore.retrieveSession("not-that")).thenReturn(Mono.empty());
		when(this.sessionIdResolver.resolveSessionIds(this.exchange)).thenReturn(ids);
		WebSession actual = this.sessionManager.getSession(this.exchange).block();

		assertNotNull(actual);
		assertEquals(this.updateSession.getId(), actual.getId());
	}
}
