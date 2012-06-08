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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * A test fixture for PathExtensionContentNegotiationStrategy.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class PathExtensionContentNegotiationStrategyTests {

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(servletRequest );
	}

	@Test
	public void resolveMediaTypesFromMapping() {
		this.servletRequest.setRequestURI("test.html");
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("text", "html")), mediaTypes);

		strategy = new PathExtensionContentNegotiationStrategy(Collections.singletonMap("HTML", "application/xhtml+xml"));
		mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("application", "xhtml+xml")), mediaTypes);
	}

	@Test
	public void resolveMediaTypesFromJaf() {
		this.servletRequest.setRequestURI("test.xls");
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("application", "vnd.ms-excel")), mediaTypes);
	}

	@Test
	public void getMediaTypeFromFilenameNoJaf() {
		this.servletRequest.setRequestURI("test.xls");
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		strategy.setUseJaf(false);

		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM), mediaTypes);
	}

	// SPR-8678

	@Test
	public void getMediaTypeFilenameWithContextPath() {
		this.servletRequest.setContextPath("/project-1.0.0.M3");
		this.servletRequest.setRequestURI("/project-1.0.0.M3/");
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		assertTrue("Context path should be excluded", strategy.resolveMediaTypes(webRequest).isEmpty());

		this.servletRequest.setRequestURI("/project-1.0.0.M3");

		assertTrue("Context path should be excluded", strategy.resolveMediaTypes(webRequest).isEmpty());
	}

	// SPR-9390

	@Test
	public void getMediaTypeFilenameWithEncodedURI() {
		this.servletRequest.setRequestURI("/quo%20vadis%3f.html");
		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		List<MediaType> result = strategy.resolveMediaTypes(webRequest);

		assertEquals("Invalid content type", Collections.singletonList(new MediaType("text", "html")), result);
	}

}
