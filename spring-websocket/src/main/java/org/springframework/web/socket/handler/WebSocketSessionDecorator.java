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

package org.springframework.web.socket.handler;


import guru.mocker.annotation.mixin.Mixin;

import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketSession;

/**
 * Wraps another {@link org.springframework.web.socket.WebSocketSession} instance
 * and delegates to it.
 *
 * <p>Also provides a {@link #getDelegate()} method to return the decorated session
 * as well as a {@link #getLastSession()} method to go through all nested delegates
 * and return the "last" session.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class WebSocketSessionDecorator extends WebSocketSessionDecoratorForwarder implements WebSocketSession {

	@Mixin
	public WebSocketSessionDecorator(WebSocketSession delegate) {
		super(delegate);
		Assert.notNull(delegate, "Delegate WebSocketSessionSession is required");
	}


	public WebSocketSession getDelegate() {
		return delegate;
	}

	public WebSocketSession getLastSession() {
		WebSocketSession result = delegate;
		while (result instanceof WebSocketSessionDecorator webSocketSessionDecorator) {
			result = webSocketSessionDecorator.getDelegate();
		}
		return result;
	}

	public static WebSocketSession unwrap(WebSocketSession session) {
		if (session instanceof WebSocketSessionDecorator webSocketSessionDecorator) {
			return webSocketSessionDecorator.getLastSession();
		}
		else {
			return session;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
