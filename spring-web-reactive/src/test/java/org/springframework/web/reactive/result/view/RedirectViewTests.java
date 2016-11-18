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

package org.springframework.web.reactive.result.view;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Tests for redirect view, and query string construction.
 * Doesn't test URL encoding, although it does check that it's called.
 *
 * @author Sebastien Deleuze
 */
public class RedirectViewTests {

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;

	private ServerWebExchange exchange;


	@Before
	public void setUp() {
		request = new MockServerHttpRequest();
		request.setContextPath("/context");
		response = new MockServerHttpResponse();
		WebSessionManager sessionManager = new DefaultWebSessionManager();
		exchange = new DefaultServerWebExchange(request, response, sessionManager);
	}

	@Test(expected = IllegalArgumentException.class)
	public void noUrlSet() throws Exception {
		RedirectView rv = new RedirectView(null);
		rv.afterPropertiesSet();
	}

	@Test
	public void defaultStatusCode() {
		String url = "http://url.somewhere.com";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(HttpStatus.SEE_OTHER, response.getStatusCode());
		assertEquals(URI.create(url), response.getHeaders().getLocation());
	}

	@Test
	public void customStatusCode() {
		RedirectView view = RedirectView
				.builder("http://url.somewhere.com")
				.statusCode(HttpStatus.FOUND)
				.build();
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(HttpStatus.FOUND, response.getStatusCode());
		assertEquals(URI.create("http://url.somewhere.com"), response.getHeaders().getLocation());
	}

	@Test
	public void contextRelative() {
		String url = "/test.html";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(URI.create("/context/test.html"), response.getHeaders().getLocation());
	}

	@Test
	public void contextRelativeQueryParam() {
		String url = "/test.html?id=1";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(URI.create("/context/test.html?id=1"), response.getHeaders().getLocation());
	}

	@Test
	public void remoteHost() {
		RedirectView view = new RedirectView("");

		assertFalse(view.isRemoteHost("http://url.somewhere.com"));
		assertFalse(view.isRemoteHost("/path"));
		assertFalse(view.isRemoteHost("http://url.somewhereelse.com"));

		view.setHosts(new String[] {"url.somewhere.com"});

		assertFalse(view.isRemoteHost("http://url.somewhere.com"));
		assertFalse(view.isRemoteHost("/path"));
		assertTrue(view.isRemoteHost("http://url.somewhereelse.com"));
	}

	@Test
	public void expandUriTemplateVariablesFromModel() {
		String url = "http://url.somewhere.com?foo={foo}";
		Map<String, String> model = Collections.singletonMap("foo", "bar");
		RedirectView view = new RedirectView(url);
		view.render(model, MediaType.TEXT_HTML, exchange);
		assertEquals(URI.create("http://url.somewhere.com?foo=bar"), response.getHeaders().getLocation());
	}

	@Test
	public void expandUriTemplateVariablesFromExchangeAttribute() {
		String url = "http://url.somewhere.com?foo={foo}";
		Map<String, String> attributes = Collections.singletonMap("foo", "bar");
		exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, attributes);
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(URI.create("http://url.somewhere.com?foo=bar"), response.getHeaders().getLocation());
	}

	@Test
	public void propagateQueryParams() throws Exception {
		RedirectView view = RedirectView
				.builder("http://url.somewhere.com?foo=bar#bazz")
				.propagateQueryParams(true)
				.build();
		request.setUri(URI.create("http://url.somewhere.com?a=b&c=d"));
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange);
		assertEquals(HttpStatus.SEE_OTHER, response.getStatusCode());
		assertEquals(URI.create("http://url.somewhere.com?foo=bar&a=b&c=d#bazz"), response.getHeaders().getLocation());
	}

}
