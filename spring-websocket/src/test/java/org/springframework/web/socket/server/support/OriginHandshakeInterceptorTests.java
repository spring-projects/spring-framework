/*
 * Copyright 2002-2015 the original author or authors.
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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;

/**
 * Test fixture for {@link OriginHandshakeInterceptor}.
 *
 * @author Sebastien Deleuze
 */
public class OriginHandshakeInterceptorTests extends AbstractHttpRequestTests {

	@Test(expected = IllegalArgumentException.class)
	public void invalidInput() {
		new OriginHandshakeInterceptor(null);
	}

	@Test
	public void originValueMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain1.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList("http://mydomain1.com"));
		assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertNotEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void originValueNoMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain1.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList("http://mydomain2.com"));
		assertFalse(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void originListMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain2.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList("http://mydomain1.com", "http://mydomain2.com", "http://mydomain3.com"));
		assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertNotEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void originListNoMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain4.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList("http://mydomain1.com", "http://mydomain2.com", "http://mydomain3.com"));
		assertFalse(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void originNoMatchWithNullHostileCollection() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain4.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		Set<String> allowedOrigins = new ConcurrentSkipListSet<String>();
		allowedOrigins.add("http://mydomain1.com");
		interceptor.setAllowedOrigins(allowedOrigins);
		assertFalse(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void originMatchAll() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain1.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		interceptor.setAllowedOrigins(Arrays.asList("*"));
		assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertNotEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void sameOriginMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain2.com");
		this.servletRequest.setServerName("mydomain2.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList());
		assertTrue(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertNotEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

	@Test
	public void sameOriginNoMatch() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		WebSocketHandler wsHandler = Mockito.mock(WebSocketHandler.class);
		setOrigin("http://mydomain3.com");
		this.servletRequest.setServerName("mydomain2.com");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList());
		assertFalse(interceptor.beforeHandshake(request, response, wsHandler, attributes));
		assertEquals(servletResponse.getStatus(), HttpStatus.FORBIDDEN.value());
	}

}
