/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link MockRestRequestMatchers}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class MockRestRequestMatchersTests {

	private final MockClientHttpRequest request = new MockClientHttpRequest();


	@Test
	public void requestTo() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/bar"));

		MockRestRequestMatchers.requestTo("http://www.foo.com/bar").match(this.request);
	}

	@Test  // SPR-15819
	public void requestToUriTemplate() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/bar"));

		MockRestRequestMatchers.requestToUriTemplate("http://www.foo.com/{bar}", "bar").match(this.request);
	}

	@Test
	public void requestToNoMatch() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/bar"));

		assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.requestTo("http://www.foo.com/wrong").match(this.request));
	}

	@Test
	public void requestToContains() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/bar"));

		MockRestRequestMatchers.requestTo(containsString("bar")).match(this.request);
	}

	@Test
	public void method() throws Exception {
		this.request.setMethod(HttpMethod.GET);

		MockRestRequestMatchers.method(HttpMethod.GET).match(this.request);
	}

	@Test
	public void methodNoMatch() throws Exception {
		this.request.setMethod(HttpMethod.POST);

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.method(HttpMethod.GET).match(this.request));
		assertThat(error.getMessage(), containsString("expected:<GET> but was:<POST>"));
	}

	@Test
	public void header() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test
	public void headerMissing() throws Exception {
		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", "bar").match(this.request));
		assertThat(error.getMessage(), containsString("was null"));
	}

	@Test
	public void headerMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", "bad").match(this.request));
		assertThat(error.getMessage(), containsString("expected:<bad> but was:<bar>"));
	}

	@Test
	public void headerContains() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", containsString("ba")).match(this.request);
	}

	@Test
	public void headerContainsWithMissingHeader() throws Exception {
		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", containsString("baz")).match(this.request));
		assertThat(error.getMessage(), containsString("but was null"));
	}

	@Test
	public void headerContainsWithMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", containsString("bx")).match(this.request));
		assertThat(error.getMessage(), containsString("was \"bar\""));
	}

	@Test
	public void headers() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test
	public void headersWithMissingHeader() throws Exception {
		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", "bar").match(this.request));
		assertThat(error.getMessage(), containsString("but was null"));
	}

	@Test
	public void headersWithMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Collections.singletonList("bar"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request));
		assertThat(error.getMessage(), containsString("to have at least <2> values"));
	}

	@Test
	public void queryParam() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/a?foo=bar&foo=baz"));

		MockRestRequestMatchers.queryParam("foo", "bar", "baz").match(this.request);
	}

	@Test
	public void queryParamMissing() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/a"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.queryParam("foo", "bar").match(this.request));
		assertThat(error.getMessage(), containsString("but was null"));
	}

	@Test
	public void queryParamMissingValue() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/a?foo=bar&foo=baz"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.queryParam("foo", "bad").match(this.request));
		assertThat(error.getMessage(), containsString("expected:<bad> but was:<bar>"));
	}

	@Test
	public void queryParamContains() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/a?foo=bar&foo=baz"));

		MockRestRequestMatchers.queryParam("foo", containsString("ba")).match(this.request);
	}

	@Test
	public void queryParamContainsWithMissingValue() throws Exception {
		this.request.setURI(new URI("http://www.foo.com/a?foo=bar&foo=baz"));

		AssertionError error = assertThrows(AssertionError.class,
				() -> MockRestRequestMatchers.queryParam("foo", containsString("bx")).match(this.request));
		assertThat(error.getMessage(), containsString("was \"bar\""));
	}

}
