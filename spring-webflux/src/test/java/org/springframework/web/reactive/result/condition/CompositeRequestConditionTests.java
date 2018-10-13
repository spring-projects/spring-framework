/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for {@link CompositeRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
public class CompositeRequestConditionTests {

	private ParamsRequestCondition param1;
	private ParamsRequestCondition param2;
	private ParamsRequestCondition param3;

	private HeadersRequestCondition header1;
	private HeadersRequestCondition header2;
	private HeadersRequestCondition header3;


	@Before
	public void setup() throws Exception {
		this.param1 = new ParamsRequestCondition("param1");
		this.param2 = new ParamsRequestCondition("param2");
		this.param3 = this.param1.combine(this.param2);

		this.header1 = new HeadersRequestCondition("header1");
		this.header2 = new HeadersRequestCondition("header2");
		this.header3 = this.header1.combine(this.header2);
	}


	@Test
	public void combine() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1, this.header1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param2, this.header2);
		CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3, this.header3);

		assertEquals(cond3, cond1.combine(cond2));
	}

	@Test
	public void combineEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);

		assertSame(empty, empty.combine(empty));
		assertSame(notEmpty, notEmpty.combine(empty));
		assertSame(notEmpty, empty.combine(notEmpty));
	}

	@Test(expected = IllegalArgumentException.class)
	public void combineDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		cond1.combine(cond2);
	}

	@Test
	public void match() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?param1=paramValue1").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestCondition<?> condition1 = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestCondition<?> condition2 = new RequestMethodsRequestCondition(RequestMethod.GET);

		CompositeRequestCondition composite1 = new CompositeRequestCondition(this.param1, condition1);
		CompositeRequestCondition composite2 = new CompositeRequestCondition(this.param1, condition2);

		assertEquals(composite2, composite1.getMatchingCondition(exchange));
	}

	@Test
	public void noMatch() {
		CompositeRequestCondition cond = new CompositeRequestCondition(this.param1);
		assertNull(cond.getMatchingCondition(MockServerWebExchange.from(MockServerHttpRequest.get("/"))));
	}

	@Test
	public void matchEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		assertSame(empty, empty.getMatchingCondition(MockServerWebExchange.from(MockServerHttpRequest.get("/"))));
	}

	@Test
	public void compare() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		assertEquals(1, cond1.compareTo(cond3, exchange));
		assertEquals(-1, cond3.compareTo(cond1, exchange));
	}

	@Test
	public void compareEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		assertEquals(0, empty.compareTo(empty, exchange));
		assertEquals(-1, notEmpty.compareTo(empty, exchange));
		assertEquals(1, empty.compareTo(notEmpty, exchange));
	}

	@Test(expected = IllegalArgumentException.class)
	public void compareDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		cond1.compareTo(cond2, MockServerWebExchange.from(MockServerHttpRequest.get("/")));
	}


}
