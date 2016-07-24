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

package org.springframework.web.reactive.result.condition;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link RequestConditionHolder}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestConditionHolderTests {

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		this.exchange = createExchange();
	}

	private ServerWebExchange createExchange() throws URISyntaxException {
		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = new MockWebSessionManager();
		return new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);
	}


	@Test
	public void combine() {
		RequestConditionHolder params1 = new RequestConditionHolder(new ParamsRequestCondition("name1"));
		RequestConditionHolder params2 = new RequestConditionHolder(new ParamsRequestCondition("name2"));
		RequestConditionHolder expected = new RequestConditionHolder(new ParamsRequestCondition("name1", "name2"));

		assertEquals(expected, params1.combine(params2));
	}

	@Test
	public void combineEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertSame(empty, empty.combine(empty));
		assertSame(notEmpty, notEmpty.combine(empty));
		assertSame(notEmpty, empty.combine(notEmpty));
	}

	@Test(expected=ClassCastException.class)
	public void combineIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		params.combine(headers);
	}

	@Test
	public void match() {
		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);
		RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(RequestMethod.GET);

		RequestConditionHolder holder = custom.getMatchingCondition(this.exchange);
		assertNotNull(holder);
		assertEquals(expected,  holder.getCondition());
	}

	@Test
	public void noMatch() {
		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);

		assertNull(custom.getMatchingCondition(this.exchange));
	}

	@Test
	public void matchEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		assertSame(empty, empty.getMatchingCondition(this.exchange));
	}

	@Test
	public void compare() {
		RequestConditionHolder params11 = new RequestConditionHolder(new ParamsRequestCondition("1"));
		RequestConditionHolder params12 = new RequestConditionHolder(new ParamsRequestCondition("1", "2"));

		assertEquals(1, params11.compareTo(params12, this.exchange));
		assertEquals(-1, params12.compareTo(params11, this.exchange));
	}

	@Test
	public void compareEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder empty2 = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertEquals(0, empty.compareTo(empty2, this.exchange));
		assertEquals(-1, notEmpty.compareTo(empty, this.exchange));
		assertEquals(1, empty.compareTo(notEmpty, this.exchange));
	}

	@Test(expected=ClassCastException.class)
	public void compareIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		params.compareTo(headers, this.exchange);
	}


}
