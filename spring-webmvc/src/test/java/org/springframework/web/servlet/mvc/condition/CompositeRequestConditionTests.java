/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.servlet.mvc.condition;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * A test fixture for {@link CompositeRequestCondition} tests.
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

	@BeforeEach
	public void setup() {
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

		assertThat(cond1.combine(cond2)).isEqualTo(cond3);
	}

	@Test
	public void combineEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);

		assertThat(empty.combine(empty)).isSameAs(empty);
		assertThat(notEmpty.combine(empty)).isSameAs(notEmpty);
		assertThat(empty.combine(notEmpty)).isSameAs(notEmpty);
	}

	@Test
	public void combineDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				cond1.combine(cond2));
	}

	@Test
	public void match() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setParameter("param1", "paramValue1");
		request.addHeader("header1", "headerValue1");

		RequestCondition<?> getPostCond = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestCondition<?> getCond = new RequestMethodsRequestCondition(RequestMethod.GET);

		CompositeRequestCondition condition = new CompositeRequestCondition(this.param1, getPostCond);
		CompositeRequestCondition matchingCondition = new CompositeRequestCondition(this.param1, getCond);

		assertThat(condition.getMatchingCondition(request)).isEqualTo(matchingCondition);
	}

	@Test
	public void noMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		CompositeRequestCondition cond = new CompositeRequestCondition(this.param1);

		assertThat(cond.getMatchingCondition(request)).isNull();
	}

	@Test
	public void matchEmpty() {
		CompositeRequestCondition empty = new CompositeRequestCondition();
		assertThat(empty.getMatchingCondition(new MockHttpServletRequest())).isSameAs(empty);
	}

	@Test
	public void compare() {
		HttpServletRequest request = new MockHttpServletRequest();

		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond3 = new CompositeRequestCondition(this.param3);

		assertThat(cond1.compareTo(cond3, request)).isEqualTo(1);
		assertThat(cond3.compareTo(cond1, request)).isEqualTo(-1);
	}

	@Test
	public void compareEmpty() {
		HttpServletRequest request = new MockHttpServletRequest();

		CompositeRequestCondition empty = new CompositeRequestCondition();
		CompositeRequestCondition notEmpty = new CompositeRequestCondition(this.param1);

		assertThat(empty.compareTo(empty, request)).isEqualTo(0);
		assertThat(notEmpty.compareTo(empty, request)).isEqualTo(-1);
		assertThat(empty.compareTo(notEmpty, request)).isEqualTo(1);
	}

	@Test
	public void compareDifferentLength() {
		CompositeRequestCondition cond1 = new CompositeRequestCondition(this.param1);
		CompositeRequestCondition cond2 = new CompositeRequestCondition(this.param1, this.header1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				cond1.compareTo(cond2, new MockHttpServletRequest()));
	}

}
