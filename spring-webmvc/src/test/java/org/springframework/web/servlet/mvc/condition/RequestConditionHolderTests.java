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
import org.junit.jupiter.api.Test;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A test fixture for {@link RequestConditionHolder} tests.
 *
 * @author Rossen Stoyanchev
 */
public class RequestConditionHolderTests {

	@Test
	public void combine() {
		RequestConditionHolder params1 = new RequestConditionHolder(new ParamsRequestCondition("name1"));
		RequestConditionHolder params2 = new RequestConditionHolder(new ParamsRequestCondition("name2"));
		RequestConditionHolder expected = new RequestConditionHolder(new ParamsRequestCondition("name1", "name2"));

		assertThat(params1.combine(params2)).isEqualTo(expected);
	}

	@Test
	public void combineEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertThat(empty.combine(empty)).isSameAs(empty);
		assertThat(notEmpty.combine(empty)).isSameAs(notEmpty);
		assertThat(empty.combine(notEmpty)).isSameAs(notEmpty);
	}

	@Test
	public void combineIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
				params.combine(headers));
	}

	@Test
	public void match() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.setParameter("name1", "value1");

		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.GET, RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);
		RequestMethodsRequestCondition expected = new RequestMethodsRequestCondition(RequestMethod.GET);

		assertThat(custom.getMatchingCondition(request).getCondition()).isEqualTo(expected);
	}

	@Test
	public void noMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

		RequestMethodsRequestCondition rm = new RequestMethodsRequestCondition(RequestMethod.POST);
		RequestConditionHolder custom = new RequestConditionHolder(rm);

		assertThat(custom.getMatchingCondition(request)).isNull();
	}

	@Test
	public void matchEmpty() {
		RequestConditionHolder empty = new RequestConditionHolder(null);
		assertThat(empty.getMatchingCondition(new MockHttpServletRequest())).isSameAs(empty);
	}

	@Test
	public void compare() {
		HttpServletRequest request = new MockHttpServletRequest();

		RequestConditionHolder params11 = new RequestConditionHolder(new ParamsRequestCondition("1"));
		RequestConditionHolder params12 = new RequestConditionHolder(new ParamsRequestCondition("1", "2"));

		assertThat(params11.compareTo(params12, request)).isEqualTo(1);
		assertThat(params12.compareTo(params11, request)).isEqualTo(-1);
	}

	@Test
	public void compareEmpty() {
		HttpServletRequest request = new MockHttpServletRequest();

		RequestConditionHolder empty = new RequestConditionHolder(null);
		RequestConditionHolder empty2 = new RequestConditionHolder(null);
		RequestConditionHolder notEmpty = new RequestConditionHolder(new ParamsRequestCondition("name"));

		assertThat(empty.compareTo(empty2, request)).isEqualTo(0);
		assertThat(notEmpty.compareTo(empty, request)).isEqualTo(-1);
		assertThat(empty.compareTo(notEmpty, request)).isEqualTo(1);
	}

	@Test
	public void compareIncompatible() {
		RequestConditionHolder params = new RequestConditionHolder(new ParamsRequestCondition("name"));
		RequestConditionHolder headers = new RequestConditionHolder(new HeadersRequestCondition("name"));
		assertThatExceptionOfType(ClassCastException.class).isThrownBy(() ->
				params.compareTo(headers, new MockHttpServletRequest()));
	}

}
