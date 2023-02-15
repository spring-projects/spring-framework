/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * Unit tests for {@link MockRestRequestMatchers}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class MockRestRequestMatchersTests {

	private final MockClientHttpRequest request = new MockClientHttpRequest();


	@Test
	void requestTo() throws Exception {
		this.request.setURI(URI.create("http://www.foo.example/bar"));

		MockRestRequestMatchers.requestTo("http://www.foo.example/bar").match(this.request);
	}

	@Test  // SPR-15819
	void requestToUriTemplate() throws Exception {
		this.request.setURI(URI.create("http://www.foo.example/bar"));

		MockRestRequestMatchers.requestToUriTemplate("http://www.foo.example/{bar}", "bar").match(this.request);
	}

	@Test
	void requestToNoMatch() {
		this.request.setURI(URI.create("http://www.foo.example/bar"));

		assertThatThrownBy(
			() -> MockRestRequestMatchers.requestTo("http://www.foo.example/wrong").match(this.request))
				.isInstanceOf(AssertionError.class);
	}

	@Test
	void requestToContains() throws Exception {
		this.request.setURI(URI.create("http://www.foo.example/bar"));

		MockRestRequestMatchers.requestTo(containsString("bar")).match(this.request);
	}

	@Test
	void method() throws Exception {
		this.request.setMethod(HttpMethod.GET);

		MockRestRequestMatchers.method(HttpMethod.GET).match(this.request);
	}

	@Test
	void methodNoMatch() {
		this.request.setMethod(HttpMethod.POST);

		assertThatThrownBy(() -> MockRestRequestMatchers.method(HttpMethod.GET).match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("expected:<GET> but was:<POST>");
	}

	@Test
	void header() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test
	void headerDoesNotExist() throws Exception {
		MockRestRequestMatchers.headerDoesNotExist(null).match(this.request);
		MockRestRequestMatchers.headerDoesNotExist("").match(this.request);
		MockRestRequestMatchers.headerDoesNotExist("foo").match(this.request);

		List<String> values = Arrays.asList("bar", "baz");
		this.request.getHeaders().put("foo", values);
		assertThatThrownBy(() -> MockRestRequestMatchers.headerDoesNotExist("foo").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessage("Expected header <foo> not to exist, but it exists with values: " + values);
	}

	@Test
	void headerMissing() {
		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("was null");
	}

	@Test
	void headerMissingValue() {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bad").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("expected:<bad> but was:<bar>");
	}

	@Test
	void headerContains() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", containsString("ba")).match(this.request);
	}

	@Test
	void headerContainsWithMissingHeader() {
		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", containsString("baz")).match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("but was null");
	}

	@Test
	void headerContainsWithMissingValue() {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", containsString("bx")).match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("was \"bar\"");
	}

	@Test
	void headerListMissing() {
		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", hasSize(2)).match(this.request))
				.isInstanceOf(AssertionError.class)
				.hasMessage("No header values for header [foo]");
	}

	@Test
	void headerListMatchers() throws IOException {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", containsInAnyOrder(endsWith("baz"), endsWith("bar"))).match(this.request);
		MockRestRequestMatchers.header("foo", contains(is("bar"), is("baz"))).match(this.request);
		MockRestRequestMatchers.header("foo", contains(is("bar"), Matchers.anything())).match(this.request);
		MockRestRequestMatchers.header("foo", hasItem(endsWith("baz"))).match(this.request);
		MockRestRequestMatchers.header("foo", everyItem(startsWith("ba"))).match(this.request);
		MockRestRequestMatchers.header("foo", hasSize(2)).match(this.request);

		//these can be a bit ambiguous when reading the test (the compiler selects the list matcher):
		MockRestRequestMatchers.header("foo", notNullValue()).match(this.request);
		MockRestRequestMatchers.header("foo", is(anything())).match(this.request);
		MockRestRequestMatchers.header("foo", allOf(notNullValue(), notNullValue())).match(this.request);

		//these are not as ambiguous thanks to an inner matcher that is either obviously list-oriented,
		//string-oriented or obviously a vararg of matchers
		//list matcher version
		MockRestRequestMatchers.header("foo", allOf(notNullValue(), hasSize(2))).match(this.request);
		//vararg version
		MockRestRequestMatchers.header("foo", allOf(notNullValue(), endsWith("ar"))).match(this.request);
		MockRestRequestMatchers.header("foo", is((any(String.class)))).match(this.request);
		MockRestRequestMatchers.header("foo", CoreMatchers.either(is("bar")).or(is(nullValue()))).match(this.request);
		MockRestRequestMatchers.header("foo", is(notNullValue()), is(notNullValue())).match(this.request);
	}

	@Test
	void headerListContainsMismatch() {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.header("foo", contains(containsString("ba"))).match(this.request))
				.withMessage("Request header values for [foo]" + System.lineSeparator()
						+ "Expected: iterable containing [a string containing \"ba\"]" + System.lineSeparator()
						+ "     but: not matched: \"baz\"");

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.header("foo", hasItem(endsWith("ba"))).match(this.request))
				.withMessage("Request header values for [foo]" + System.lineSeparator()
						+ "Expected: a collection containing a string ending with \"ba\"" + System.lineSeparator()
						+ "     but: mismatches were: [was \"bar\", was \"baz\"]");

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.header("foo", everyItem(endsWith("ar"))).match(this.request))
				.withMessage("Request header values for [foo]" + System.lineSeparator()
						+ "Expected: every item is a string ending with \"ar\"" + System.lineSeparator()
						+ "     but: an item was \"baz\"");
	}

	@Test
	void headers() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test
	void headersWithMissingHeader() {
		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("but was null");
	}

	@Test
	void headersWithMissingValue() {
		this.request.getHeaders().put("foo", Collections.singletonList("bar"));

		assertThatThrownBy(() -> MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("to have at least <2> values");
	}

	@Test
	void queryParam() throws Exception {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		MockRestRequestMatchers.queryParam("foo", "bar", "baz").match(this.request);
	}

	@Test
	void queryParamMissing() {
		this.request.setURI(URI.create("http://www.foo.example/a"));

		assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", "bar").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("but was null");
	}

	@Test
	void queryParamMissingValue() {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", "bad").match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("expected:<bad> but was:<bar>");
	}

	@Test
	void queryParamContains() throws Exception {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		MockRestRequestMatchers.queryParam("foo", containsString("ba")).match(this.request);
	}

	@Test
	void queryParamContainsWithMissingValue() {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", containsString("bx")).match(this.request))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("was \"bar\"");
	}


	@Test
	void queryParamListMissing() {
		assertThatThrownBy(() -> MockRestRequestMatchers.queryParam("foo", hasSize(2)).match(this.request))
				.isInstanceOf(AssertionError.class)
				.hasMessage("No queryParam [foo]");
	}

	@Test
	void queryParamListMatchers() throws IOException {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		MockRestRequestMatchers.queryParam("foo", containsInAnyOrder(endsWith("baz"), endsWith("bar"))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", contains(is("bar"), is("baz"))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", contains(is("bar"), Matchers.anything())).match(this.request);
		MockRestRequestMatchers.queryParam("foo", hasItem(endsWith("baz"))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", everyItem(startsWith("ba"))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", hasSize(2)).match(this.request);

		//these can be a bit ambiguous when reading the test (the compiler selects the list matcher):
		MockRestRequestMatchers.queryParam("foo", notNullValue()).match(this.request);
		MockRestRequestMatchers.queryParam("foo", is(anything())).match(this.request);
		MockRestRequestMatchers.queryParam("foo", allOf(notNullValue(), notNullValue())).match(this.request);

		//these are not as ambiguous thanks to an inner matcher that is either obviously list-oriented,
		//string-oriented or obviously a vararg of matchers
		//list matcher version
		MockRestRequestMatchers.queryParam("foo", allOf(notNullValue(), hasSize(2))).match(this.request);
		//vararg version
		MockRestRequestMatchers.queryParam("foo", allOf(notNullValue(), endsWith("ar"))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", is((any(String.class)))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", CoreMatchers.either(is("bar")).or(is(nullValue()))).match(this.request);
		MockRestRequestMatchers.queryParam("foo", is(notNullValue()), is(notNullValue())).match(this.request);
	}

	@Test
	void queryParamListContainsMismatch() {
		this.request.setURI(URI.create("http://www.foo.example/a?foo=bar&foo=baz"));

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.queryParam("foo", contains(containsString("ba"))).match(this.request))
				.withMessage("Request queryParam values for [foo]" + System.lineSeparator()
						+ "Expected: iterable containing [a string containing \"ba\"]" + System.lineSeparator()
						+ "     but: not matched: \"baz\"");

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.queryParam("foo", hasItem(endsWith("ba"))).match(this.request))
				.withMessage("Request queryParam values for [foo]" + System.lineSeparator()
						+ "Expected: a collection containing a string ending with \"ba\"" + System.lineSeparator()
						+ "     but: mismatches were: [was \"bar\", was \"baz\"]");

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> MockRestRequestMatchers
						.queryParam("foo", everyItem(endsWith("ar"))).match(this.request))
				.withMessage("Request queryParam values for [foo]" + System.lineSeparator()
						+ "Expected: every item is a string ending with \"ar\"" + System.lineSeparator()
						+ "     but: an item was \"baz\"");
	}

}
