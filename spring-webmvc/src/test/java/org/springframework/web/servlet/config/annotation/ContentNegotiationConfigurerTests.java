/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.Assert.*;

/**
 * Test fixture for {@link ContentNegotiationConfigurer} tests.
 * @author Rossen Stoyanchev
 */
public class ContentNegotiationConfigurerTests {

	private ContentNegotiationConfigurer configurer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest);
		this.configurer = new ContentNegotiationConfigurer(this.servletRequest.getServletContext());
	}

	@Test
	public void defaultSettings() throws Exception {
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower.gif");

		assertEquals("Should be able to resolve file extensions by default",
				Arrays.asList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower?format=gif");
		this.servletRequest.addParameter("format", "gif");

		assertEquals("Should not resolve request parameters by default",
				Collections.emptyList(), manager.resolveMediaTypes(this.webRequest));

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertEquals("Should resolve Accept header by default",
				Arrays.asList(MediaType.IMAGE_GIF), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void addMediaTypes() throws Exception {
		this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower.json");
		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void favorParameter() throws Exception {
		this.configurer.favorParameter(true);
		this.configurer.parameterName("f");
		this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("f", "json");

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void ignoreAcceptHeader() throws Exception {
		this.configurer.ignoreAcceptHeader(true);
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertEquals(Collections.emptyList(), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void setDefaultContentType() throws Exception {
		this.configurer.defaultContentType(MediaType.APPLICATION_JSON);
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}

	@Test
	public void setDefaultContentTypeStrategy() throws Exception {
		this.configurer.defaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.getContentNegotiationManager();

		assertEquals(Arrays.asList(MediaType.APPLICATION_JSON), manager.resolveMediaTypes(this.webRequest));
	}
}
