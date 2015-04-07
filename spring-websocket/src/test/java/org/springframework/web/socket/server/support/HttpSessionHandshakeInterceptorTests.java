/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket.server.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.mock.web.test.MockHttpSession;
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
	public void defaultConstructor() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertEquals(3, attributes.size());
		assertEquals("bar", attributes.get("foo"));
		assertEquals("baz", attributes.get("bar"));
		assertEquals("123", attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME));
	}

	@Test
	public void constructorWithAttributeNames() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		Set<String> names = Collections.singleton("foo");
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor(names);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertEquals(2, attributes.size());
		assertEquals("bar", attributes.get("foo"));
		assertEquals("123", attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME));
	}

	@Test
	public void doNotCopyHttpSessionId() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.setCopyHttpSessionId(false);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertEquals(1, attributes.size());
		assertEquals("bar", attributes.get("foo"));
	}


	@Test
	public void doNotCopyAttributes() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.setCopyAllAttributes(false);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertEquals(1, attributes.size());
		assertEquals("123", attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME));
	}

	@Test
	public void doNotCauseSessionCreation() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertNull(this.servletRequest.getSession(false));
	}

}
