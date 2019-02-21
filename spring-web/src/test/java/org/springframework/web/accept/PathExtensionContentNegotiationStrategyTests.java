/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

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
		this.webRequest = new ServletWebRequest(servletRequest);
	}


	@Test
	public void resolveMediaTypesFromMapping() throws Exception {

		this.servletRequest.setRequestURI("test.html");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("text", "html")), mediaTypes);

		Map<String, MediaType> mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		strategy = new PathExtensionContentNegotiationStrategy(mapping);
		mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("application", "xhtml+xml")), mediaTypes);
	}

	@Test
	public void resolveMediaTypesFromMediaTypeFactory() throws Exception {

		this.servletRequest.setRequestURI("test.xls");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(Arrays.asList(new MediaType("application", "vnd.ms-excel")), mediaTypes);
	}

	// SPR-8678

	@Test
	public void getMediaTypeFilenameWithContextPath() throws Exception {

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		this.servletRequest.setContextPath("/project-1.0.0.M3");
		this.servletRequest.setRequestURI("/project-1.0.0.M3/");
		assertEquals("Context path should be excluded", ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST,
				strategy.resolveMediaTypes(webRequest));

		this.servletRequest.setRequestURI("/project-1.0.0.M3");
		assertEquals("Context path should be excluded", ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST,
				strategy.resolveMediaTypes(webRequest));
	}

	// SPR-9390

	@Test
	public void getMediaTypeFilenameWithEncodedURI() throws Exception {

		this.servletRequest.setRequestURI("/quo%20vadis%3f.html");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> result = strategy.resolveMediaTypes(webRequest);

		assertEquals("Invalid content type", Collections.singletonList(new MediaType("text", "html")), result);
	}

	// SPR-10170

	@Test
	public void resolveMediaTypesIgnoreUnknownExtension() throws Exception {

		this.servletRequest.setRequestURI("test.foobar");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertEquals(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST, mediaTypes);
	}

	@Test(expected = HttpMediaTypeNotAcceptableException.class)
	public void resolveMediaTypesDoNotIgnoreUnknownExtension() throws Exception {

		this.servletRequest.setRequestURI("test.foobar");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		strategy.setIgnoreUnknownExtensions(false);
		strategy.resolveMediaTypes(this.webRequest);
	}

}
