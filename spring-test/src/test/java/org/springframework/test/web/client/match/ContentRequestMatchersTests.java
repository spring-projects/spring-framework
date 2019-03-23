/*
 * Copyright 2002-2016 the original author or authors.
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

import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.Matchers.*;

/**
 * Unit tests for {@link ContentRequestMatchers}.
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

		MockRestRequestMatchers.content().contentType("application/json").match(this.request);
		MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_JSON).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testContentTypeNoMatch1() throws Exception {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		MockRestRequestMatchers.content().contentType("application/xml").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testContentTypeNoMatch2() throws Exception {
		this.request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		MockRestRequestMatchers.content().contentType(MediaType.APPLICATION_ATOM_XML).match(this.request);
	}

	@Test
	public void testString() throws Exception {
		this.request.getBody().write("test".getBytes());

		MockRestRequestMatchers.content().string("test").match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testStringNoMatch() throws Exception {
		this.request.getBody().write("test".getBytes());

		MockRestRequestMatchers.content().string("Test").match(this.request);
	}

	@Test
	public void testBytes() throws Exception {
		byte[] content = "test".getBytes();
		this.request.getBody().write(content);

		MockRestRequestMatchers.content().bytes(content).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testBytesNoMatch() throws Exception {
		this.request.getBody().write("test".getBytes());

		MockRestRequestMatchers.content().bytes("Test".getBytes()).match(this.request);
	}

	@Test
	public void testFormData() throws Exception {
		String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
		String body = "name+1=value+1&name+2=value+A&name+2=value+B&name+3";

		this.request.getHeaders().setContentType(MediaType.parseMediaType(contentType));
		this.request.getBody().write(body.getBytes(Charset.forName("UTF-8")));

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("name 1", "value 1");
		map.add("name 2", "value A");
		map.add("name 2", "value B");
		map.add("name 3", null);
		MockRestRequestMatchers.content().formData(map).match(this.request);
	}

	@Test
	public void testXml() throws Exception {
		String content = "<foo><bar>baz</bar><bar>bazz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		MockRestRequestMatchers.content().xml(content).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testXmlNoMatch() throws Exception {
		this.request.getBody().write("<foo>11</foo>".getBytes());

		MockRestRequestMatchers.content().xml("<foo>22</foo>").match(this.request);
	}

	@Test
	public void testNodeMatcher() throws Exception {
		String content = "<foo><bar>baz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		MockRestRequestMatchers.content().node(hasXPath("/foo/bar")).match(this.request);
	}

	@Test(expected = AssertionError.class)
	public void testNodeMatcherNoMatch() throws Exception {
		String content = "<foo><bar>baz</bar></foo>";
		this.request.getBody().write(content.getBytes());

		MockRestRequestMatchers.content().node(hasXPath("/foo/bar/bar")).match(this.request);
	}

}
