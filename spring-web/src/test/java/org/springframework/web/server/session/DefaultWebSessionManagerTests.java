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
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

	private DefaultWebSessionManager manager;

	private ServerWebExchange exchange;

	@Mock
	private WebSessionIdResolver idResolver;

	@Mock
	private WebSessionStore store;

	@Mock
	private WebSession createSession;

	@Mock
	private WebSession updateSession;

	
	@Before
	public void setUp() throws Exception {
		when(this.store.createWebSession()).thenReturn(Mono.just(this.createSession));
		when(this.createSession.save()).thenReturn(Mono.empty());
		when(this.updateSession.getId()).thenReturn("update-session-id");

		this.manager = new DefaultWebSessionManager();
		this.manager.setSessionIdResolver(this.idResolver);
		this.manager.setSessionStore(this.store);

		MockServerHttpRequest request = MockServerHttpRequest.get("/path").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response, this.manager,
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}

	@Test
	public void getSessionSaveWhenCreatedAndNotStartedThenNotSaved() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		this.exchange.getResponse().setComplete().block();

		assertFalse(session.isStarted());
		assertFalse(session.isExpired());
		verify(this.createSession, never()).save();
		verify(this.idResolver, never()).setSessionId(any(), any());
	}

	@Test
	public void getSessionSaveWhenCreatedAndStartedThenSavesAndSetsId() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		WebSession session = this.manager.getSession(this.exchange).block();
		when(this.createSession.isStarted()).thenReturn(true);
		this.exchange.getResponse().setComplete().block();

		String id = session.getId();
		verify(this.store).createWebSession();
		verify(this.createSession).save();
		verify(this.idResolver).setSessionId(any(), eq(id));
	}

	@Test
	public void exchangeWhenResponseSetCompleteThenSavesAndSetsId() throws Exception {
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.emptyList());
		String id = this.createSession.getId();
		WebSession session = this.manager.getSession(this.exchange).block();
		when(this.createSession.isStarted()).thenReturn(true);
		this.exchange.getResponse().setComplete().block();

		verify(this.idResolver).setSessionId(any(), eq(id));
		verify(this.createSession).save();
	}

	@Test
	public void existingSession() throws Exception {
		String id = this.updateSession.getId();
		when(this.store.retrieveSession(id)).thenReturn(Mono.just(this.updateSession));
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Collections.singletonList(id));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(id, actual.getId());
	}

	@Test
	public void multipleSessionIds() throws Exception {
		WebSession existing = this.updateSession;
		String id = existing.getId();
		when(this.store.retrieveSession(any())).thenReturn(Mono.empty());
		when(this.store.retrieveSession(id)).thenReturn(Mono.just(existing));
		when(this.idResolver.resolveSessionIds(this.exchange)).thenReturn(Arrays.asList("neither-this", "nor-that", id));

		WebSession actual = this.manager.getSession(this.exchange).block();
		assertNotNull(actual);
		assertEquals(existing.getId(), actual.getId());
	}
}
