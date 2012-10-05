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
package org.springframework.test.web.mock.client.match;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.mock.client.match.JsonPathRequestMatchers;

/**
 * Tests for {@link JsonPathRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class JsonPathRequestMatchersTests {

	private static final String RESPONSE_CONTENT = "{\"foo\":\"bar\", \"qux\":[\"baz1\",\"baz2\"]}";

	private MockClientHttpRequest request;

	@Before
	public void setUp() throws IOException {
		this.request = new MockClientHttpRequest();
		this.request.getBody().write(RESPONSE_CONTENT.getBytes());
	}

	@Test
	public void value() throws Exception {
		new JsonPathRequestMatchers("$.foo").value("bar").match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void valueNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.foo").value("bogus").match(this.request);
	}

	@Test
	public void valueMatcher() throws Exception {
		new JsonPathRequestMatchers("$.foo").value(Matchers.equalTo("bar")).match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void valueMatcherNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.foo").value(Matchers.equalTo("bogus")).match(this.request);
	}

	@Test
	public void exists() throws Exception {
		new JsonPathRequestMatchers("$.foo").exists().match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void existsNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.bogus").exists().match(this.request);
	}

	@Test
	public void doesNotExist() throws Exception {
		new JsonPathRequestMatchers("$.bogus").doesNotExist().match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void doesNotExistNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.foo").doesNotExist().match(this.request);
	}

	@Test
	public void isArrayMatch() throws Exception {
		new JsonPathRequestMatchers("$.qux").isArray().match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void isArrayNoMatch() throws Exception {
		new JsonPathRequestMatchers("$.bar").isArray().match(this.request);
	}

}
