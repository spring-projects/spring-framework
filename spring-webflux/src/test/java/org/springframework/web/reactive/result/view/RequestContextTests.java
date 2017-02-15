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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link RequestContext}.
 * @author Rossen Stoyanchev
 */
public class RequestContextTests {

	private ServerWebExchange exchange;

	private GenericApplicationContext applicationContext;

	private Map<String, Object> model = new HashMap<>();


	@Before
	public void init() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").contextPath("foo/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(request, response);
		this.applicationContext = new GenericApplicationContext();
		this.applicationContext.refresh();
	}

	@Test
	public void testGetContextUrl() throws Exception {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		assertEquals("foo/bar", context.getContextUrl("bar"));
	}

	@Test
	public void testGetContextUrlWithMap() throws Exception {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("spam", "bucket");
		assertEquals("foo/bar?spam=bucket", context.getContextUrl("{foo}?spam={spam}", map));
	}

	@Test
	public void testGetContextUrlWithMapEscaping() throws Exception {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar baz");
		map.put("spam", "&bucket=");
		assertEquals("foo/bar%20baz?spam=%26bucket%3D", context.getContextUrl("{foo}?spam={spam}", map));
	}

}
