/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.testfixture.servlet.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link HttpSessionHandshakeInterceptor}.
 *
 * @author Rossen Stoyanchev
 */
class HttpSessionHandshakeInterceptorTests extends AbstractHttpRequestTests {

	private final Map<String, Object> attributes = new HashMap<>();
	private final WebSocketHandler wsHandler = mock();


	@Test
	void defaultConstructor() throws Exception {
		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertThat(attributes).hasSize(3);
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.get("bar")).isEqualTo("baz");
		assertThat(attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME)).isEqualTo("123");
	}

	@Test
	void constructorWithAttributeNames() throws Exception {
		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");
		this.servletRequest.getSession().setAttribute("bar", "baz");

		Set<String> names = Collections.singleton("foo");
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor(names);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertThat(attributes).hasSize(2);
		assertThat(attributes.get("foo")).isEqualTo("bar");
		assertThat(attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME)).isEqualTo("123");
	}

	@Test
	void doNotCopyHttpSessionId() throws Exception {
		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.setCopyHttpSessionId(false);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertThat(attributes).hasSize(1);
		assertThat(attributes.get("foo")).isEqualTo("bar");
	}


	@Test
	void doNotCopyAttributes() throws Exception {
		this.servletRequest.setSession(new MockHttpSession(null, "123"));
		this.servletRequest.getSession().setAttribute("foo", "bar");

		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.setCopyAllAttributes(false);
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertThat(attributes).hasSize(1);
		assertThat(attributes.get(HttpSessionHandshakeInterceptor.HTTP_SESSION_ID_ATTR_NAME)).isEqualTo("123");
	}

	@Test
	void doNotCauseSessionCreation() throws Exception {
		HttpSessionHandshakeInterceptor interceptor = new HttpSessionHandshakeInterceptor();
		interceptor.beforeHandshake(this.request, this.response, wsHandler, attributes);

		assertThat(this.servletRequest.getSession(false)).isNull();
	}

}
