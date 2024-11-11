/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.result.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link CompositeRequestCondition}.
 *
 * @author Rossen Stoyanchev
 */
class CompositeRequestConditionTests {

	private ParamsRequestCondition param1;
	private ParamsRequestCondition param2;
	private ParamsRequestCondition param3;

	private HeadersRequestCondition header1;
	private HeadersRequestCondition header2;
	private HeadersRequestCondition header3;


	@BeforeEach
	void setup() {
		this.param1 = new ParamsRequestCondition("param1");
		this.param2 = new ParamsRequestCondition("param2");
		this.param3 = this.param1.combine(this.param2);

		this.header1 = new HeadersRequestCondition("header1");
		this.header2 = new HeadersRequestCondition("header2");
		this.header3 = this.header1.combine(this.header2);
	}


	@Test
	void combine() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1, this.header1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param2, this.header2);
		CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3, this.header3);

		assertThat(cond1.combine(cond2)).isEqualTo(cond3);
	}

	@Test
	void combineEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);

		assertThat(empty.combine(empty)).isSameAs(empty);
		assertThat(notEmpty.combine(empty)).isSameAs(notEmpty);
		assertThat(empty.combine(notEmpty)).isSameAs(notEmpty);
	}

	@Test
	void combineDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				cond1.combine(cond2));
	}

	@Test
	void match() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?param1=paramValue1").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);

		RequestCondition<?> condition1 = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestCondition<?> condition2 = new RequestMethodsRequestCondition(RequestMethod.GET);

		CompositeRequestCondition composite1 = new CompositeRequestCondition(this.param1, condition1);
		CompositeRequestCondition composite2 = new CompositeRequestCondition(this.param1, condition2);

		assertThat(composite1.getMatchingCondition(exchange)).isEqualTo(composite2);
	}

	@Test
	void noMatch() {
		CompositeRequestCondition cond = new CompositeRequestCondition(this.param1);
		assertThat(cond.getMatchingCondition(MockServerWebExchange.from(MockServerHttpRequest.get("/")))).isNull();
	}

	@Test
	void matchEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		assertThat(empty.getMatchingCondition(MockServerWebExchange.from(MockServerHttpRequest.get("/")))).isSameAs(empty);
	}

	@Test
	void compare() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		assertThat(cond1.compareTo(cond3, exchange)).isEqualTo(1);
		assertThat(cond3.compareTo(cond1, exchange)).isEqualTo(-1);
	}

	@Test
	void compareEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

		assertThat(empty.compareTo(empty, exchange)).isEqualTo(0);
		assertThat(notEmpty.compareTo(empty, exchange)).isEqualTo(-1);
		assertThat(empty.compareTo(notEmpty, exchange)).isEqualTo(1);
	}

	@Test
	void compareDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				cond1.compareTo(cond2, MockServerWebExchange.from(MockServerHttpRequest.get("/"))));
	}


}
