/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.reactive.result.condition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Arjen Poutsma
 */
public class ParamsRequestConditionTests {

	@Test
	public void paramEquals() {
		assertEquals(new ParamsRequestCondition("foo"), new ParamsRequestCondition("foo"));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("bar")));
		assertFalse(new ParamsRequestCondition("foo").equals(new ParamsRequestCondition("FOO")));
		assertEquals(new ParamsRequestCondition("foo=bar"), new ParamsRequestCondition("foo=bar"));
		assertFalse(new ParamsRequestCondition("foo=bar").equals(new ParamsRequestCondition("FOO=bar")));
	}

	@Test
	public void paramPresent() throws Exception {
		ServerWebExchange exchange = createExchange("foo", "");
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void paramPresentNoMatch() throws Exception {
		ServerWebExchange exchange = createExchange("bar", "");
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void paramNotPresent() throws Exception {
		ServerWebExchange exchange = createExchange();
		ParamsRequestCondition condition = new ParamsRequestCondition("!foo");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void paramValueMatch() throws Exception {
		ServerWebExchange exchange = createExchange("foo", "bar");
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		assertNotNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void paramValueNoMatch() throws Exception {
		ServerWebExchange exchange = createExchange("foo", "bazz");
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		assertNull(condition.getMatchingCondition(exchange));
	}

	@Test
	public void compareTo() throws Exception {
		ServerWebExchange exchange = createExchange();

		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo", "bar", "baz");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo", "bar");

		int result = condition1.compareTo(condition2, exchange);
		assertTrue("Invalid comparison result: " + result, result < 0);

		result = condition2.compareTo(condition1, exchange);
		assertTrue("Invalid comparison result: " + result, result > 0);
	}

	@Test
	public void combine() {
		ParamsRequestCondition condition1 = new ParamsRequestCondition("foo=bar");
		ParamsRequestCondition condition2 = new ParamsRequestCondition("foo=baz");

		ParamsRequestCondition result = condition1.combine(condition2);
		Collection<?> conditions = result.getContent();
		assertEquals(2, conditions.size());
	}

	@Test
	public void getMatchingCondition() throws Exception {
		ServerWebExchange exchange = createExchange("foo", "bar");
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		ParamsRequestCondition result = condition.getMatchingCondition(exchange);
		assertEquals(condition, result);

		condition = new ParamsRequestCondition("bar");

		result = condition.getMatchingCondition(exchange);
		assertNull(result);
	}

	private ServerWebExchange createExchange() throws URISyntaxException {
		return createExchange(null, null);
	}

	private ServerWebExchange createExchange(String paramName, String paramValue) throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		if (paramName != null) {
			request.getQueryParams().add(paramName, paramValue);
		}
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}

}
