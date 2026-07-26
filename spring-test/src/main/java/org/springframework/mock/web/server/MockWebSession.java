/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.web.server;

import java.time.Clock;

import guru.mocker.annotation.mixin.Mixin;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.InMemoryWebSessionStore;

/**
 * Implementation of {@code WebSession} that delegates to a session instance
 * obtained via {@link InMemoryWebSessionStore}.
 *
 * <p>This is intended for use with the
 * {@link MockServerWebExchange.Builder#session(WebSession) session(WebSession)}
 * method of the {@code MockServerWebExchange} builder, eliminating the need
 * to use {@code WebSessionManager} or {@code WebSessionStore} altogether.
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class MockWebSession extends MockWebSessionForwarder implements WebSession {

	public MockWebSession() {
		this((Clock) null);
	}

	@Mixin
	MockWebSession(WebSession delegate) {
		super(delegate);
	}

	public MockWebSession(@Nullable Clock clock) {
		this(createSession(clock));
	}

	private static WebSession createSession(@Nullable Clock clock) {
		InMemoryWebSessionStore sessionStore = new InMemoryWebSessionStore();
		if (clock != null) {
			sessionStore.setClock(clock);
		}
		WebSession session = sessionStore.createWebSession().block();
		Assert.state(session != null, "WebSession must not be null");
		return session;
	}

}
