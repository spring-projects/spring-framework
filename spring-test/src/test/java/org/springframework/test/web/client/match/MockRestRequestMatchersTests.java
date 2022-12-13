/*
 * Copyright 2002-2022 the original author or authors.
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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;

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

}
