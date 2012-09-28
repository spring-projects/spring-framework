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

import static org.hamcrest.Matchers.hasXPath;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.mock.client.match.ContentRequestMatchers;
import org.springframework.test.web.mock.client.match.RequestMatchers;

/**
 * Tests for {@link ContentRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class ContentRequestMatchersTests {

	private MockClientHttpRequest request;

	@Before
	public void setUp() {
		this.request = new MockClientHttpRequest();
	}

	@Test
	public void testContentType() throws Exception {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		RequestMatchers.content().mimeType("application/json").match(this.request);
		RequestMatchers.content().mimeType(MediaType.APPLICATION_JSON).match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testContentTypeNoMatch1() throws Exception {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		RequestMatchers.content().mimeType("application/xml").match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testContentTypeNoMatch2() throws Exception {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		RequestMatchers.content().mimeType(MediaType.APPLICATION_ATOM_XML).match(this.request);
	}

	@Test
	public void testString() throws Exception {
		this.request.getBody().write("test".getBytes());

		RequestMatchers.content().string("test").match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testStringNoMatch() throws Exception {
		this.request.getBody().write("test".getBytes());

		RequestMatchers.content().string("Test").match(this.request);
	}

	@Test
	public void testBytes() throws Exception {
		byte[] content = "test".getBytes();
		this.request.getBody().write(content);

		RequestMatchers.content().bytes(content).match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testBytesNoMatch() throws Exception {
		this.request.getBody().write("test".getBytes());

		RequestMatchers.content().bytes("Test".getBytes()).match(this.request);
	}

	@Test
	public void testXml() throws Exception {
		String content = "<foo><bar>baz</bar><bar>bazz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		RequestMatchers.content().xml(content).match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testXmlNoMatch() throws Exception {
		this.request.getBody().write("<foo>11</foo>".getBytes());

		RequestMatchers.content().xml("<foo>22</foo>").match(this.request);
	}

	@Test
	public void testNodeMatcher() throws Exception {
		String content = "<foo><bar>baz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		RequestMatchers.content().node(hasXPath("/foo/bar")).match(this.request);
	}

	@Test(expected=AssertionError.class)
	public void testNodeMatcherNoMatch() throws Exception {
		String content = "<foo><bar>baz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		RequestMatchers.content().node(hasXPath("/foo/bar/bar")).match(this.request);
	}

}
