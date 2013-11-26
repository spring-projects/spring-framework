/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.server.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link HttpSessionHandshakeInterceptor}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpSessionHandshakeInterceptorTests extends AbstractHttpRequestTests {


	@Test
	public void copyAllAttributes() throws Exception {

		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertEquals(2, attributes.size());
		assertEquals("bar", attributes.get("foo"));
		assertEquals("baz", attributes.get("bar"));
	}

	@Test
	public void copySelectedAttributes() throws Exception {

		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		Set<String> names = Collections.singleton("foo");
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor(names);
		interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertEquals(1, attributes.size());
		assertEquals("bar", attributes.get("foo"));
	}

	@Test
	public void doNotCauseSessionCreation() throws Exception {

		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(request, response, wsHandler, attributes);

		assertNull(this.servletRequest.getSession(false));
	}

}
