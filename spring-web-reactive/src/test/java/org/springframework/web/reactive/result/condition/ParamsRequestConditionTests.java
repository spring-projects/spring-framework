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

import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.util.UriComponentsBuilder.fromPath;

/**
 * Unit tests for {@link ParamsRequestCondition}.
 * @author Rossen Stoyanchev
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
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		assertNotNull(condition.getMatchingCondition(exchangeWithQuery("foo=")));
		assertNotNull(condition.getMatchingCondition(exchangeWithFormData("foo=")));
	}

	@Test
	public void paramPresentNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo");

		assertNull(condition.getMatchingCondition(exchangeWithQuery("bar=")));
		assertNull(condition.getMatchingCondition(exchangeWithFormData("bar=")));
	}

	@Test
	public void paramNotPresent() throws Exception {
		ServerWebExchange exchange = exchange();
		assertNotNull(new ParamsRequestCondition("!foo").getMatchingCondition(exchange));
	}

	@Test
	public void paramValueMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		assertNotNull(condition.getMatchingCondition(exchangeWithQuery("foo=bar")));
		assertNotNull(condition.getMatchingCondition(exchangeWithFormData("foo=bar")));
	}

	@Test
	public void paramValueNoMatch() throws Exception {
		ParamsRequestCondition condition = new ParamsRequestCondition("foo=bar");

		assertNull(condition.getMatchingCondition(exchangeWithQuery("foo=bazz")));
		assertNull(condition.getMatchingCondition(exchangeWithFormData("foo=bazz")));
	}

	@Test
	public void compareTo() throws Exception {
		ServerWebExchange exchange = exchange(new MockServerHttpRequest(HttpMethod.GET, "/"));

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


	private ServerWebExchange exchangeWithQuery(String query) throws URISyntaxException {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/");
		MultiValueMap<String, String> params = fromPath("/").query(query).build().getQueryParams();
		request.getQueryParams().putAll(params);
		return exchange(request);
	}

	private ServerWebExchange exchangeWithFormData(String formData) throws URISyntaxException {
		MockServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/");
		request.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		request.setBody(formData);
		return exchange(request);
	}

	private ServerWebExchange exchange() {
		return exchange(new MockServerHttpRequest(HttpMethod.GET, "/"));
	}

	private ServerWebExchange exchange(ServerHttpRequest request) {
		MockServerHttpResponse response = new MockServerHttpResponse();
		WebSessionManager manager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, response, manager);
	}

}
