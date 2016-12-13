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

package org.springframework.test.web.client.match;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link MockRestRequestMatchers}.
 *
 * @author Craig Walls
 * @author Rossen Stoyanchev
 */
public class MockRestRequestMatchersTests {

	private final MockClientHttpRequest request = new MockClientHttpRequest();


	@Test
	public void requestTo() throws Exception {
		this.request.setURI(new URI("http://foo.com/bar"));

		MockRestRequestMatchers.requestTo("http://foo.com/bar").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void requestToNoMatch() throws Exception {
		this.request.setURI(new URI("http://foo.com/bar"));

		MockRestRequestMatchers.requestTo("http://foo.com/wrong").match(this.request);
	}

	@Test
	public void requestToContains() throws Exception {
		this.request.setURI(new URI("http://foo.com/bar"));

		MockRestRequestMatchers.requestTo(containsString("bar")).match(this.request);
	}

	@Test
	public void method() throws Exception {
		this.request.setMethod(HttpMethod.GET);

		MockRestRequestMatchers.method(HttpMethod.GET).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void methodNoMatch() throws Exception {
		this.request.setMethod(HttpMethod.POST);

		MockRestRequestMatchers.method(HttpMethod.GET).match(this.request);
	}

	@Test
	public void header() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headerMissing() throws Exception {
		MockRestRequestMatchers.header("foo", "bar").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headerMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bad").match(this.request);
	}

	@Test
	public void headerContains() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", containsString("ba")).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headerContainsWithMissingHeader() throws Exception {
		MockRestRequestMatchers.header("foo", containsString("baz")).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headerContainsWithMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", containsString("bx")).match(this.request);
	}

	@Test
	public void headers() throws Exception {
		this.request.getHeaders().put("foo", Arrays.asList("bar", "baz"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headersWithMissingHeader() throws Exception {
		MockRestRequestMatchers.header("foo", "bar").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void headersWithMissingValue() throws Exception {
		this.request.getHeaders().put("foo", Collections.singletonList("bar"));

		MockRestRequestMatchers.header("foo", "bar", "baz").match(this.request);
	}

	@Test
	public void queryParam() throws Exception {
		this.request.setURI(new URI("http://foo.com/a?foo=bar&foo=baz"));
		MockRestRequestMatchers.queryParam("foo", "bar", "baz").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void queryParamMissing() throws Exception {
		this.request.setURI(new URI("http://foo.com/a"));
		MockRestRequestMatchers.queryParam("foo", "bar").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void queryParamMissingValue() throws Exception {
		this.request.setURI(new URI("http://foo.com/a?foo=bar&foo=baz"));
		MockRestRequestMatchers.queryParam("foo", "bad").match(this.request);
	}

	@Test
	public void queryParamContains() throws Exception {
		this.request.setURI(new URI("http://foo.com/a?foo=bar&foo=baz"));
		MockRestRequestMatchers.queryParam("foo", containsString("ba")).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void queryParamContainsWithMissingValue() throws Exception {
		this.request.setURI(new URI("http://foo.com/a?foo=bar&foo=baz"));
		MockRestRequestMatchers.queryParam("foo", containsString("bx")).match(this.request);
	}

}