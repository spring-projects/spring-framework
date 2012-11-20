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
package org.springframework.web.accept;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Test fixture for HeaderContentNegotiationStrategy tests.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderContentNegotiationStrategyTests {

	private HeaderContentNegotiationStrategy strategy;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@Before
	public void setup() {
		this.strategy = new HeaderContentNegotiationStrategy();
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(servletRequest );
	}

	@Test
	public void resolveMediaTypes() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertEquals(4, mediaTypes.size());
		assertEquals("text/html", mediaTypes.get(0).toString());
		assertEquals("text/x-c", mediaTypes.get(1).toString());
		assertEquals("text/x-dvi;q=0.8", mediaTypes.get(2).toString());
		assertEquals("text/plain;q=0.5", mediaTypes.get(3).toString());
	}

	@Test(expected=HttpMediaTypeNotAcceptableException.class)
	public void resolveMediaTypesParseError() throws Exception {
		this.servletRequest.addHeader("Accept", "textplain; q=0.5");
		this.strategy.resolveMediaTypes(this.webRequest);
	}

}
