/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link ContentNegotiationConfigurer} tests.
 * @author Rossen Stoyanchev
 */
public class ContentNegotiationConfigurerTests {

	private ContentNegotiationConfigurer configurer;

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;


	@BeforeEach
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest);
		this.configurer = new ContentNegotiationConfigurer(this.servletRequest.getServletContext());
	}


	@Test
	public void defaultSettings() throws Exception {
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower.gif");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should not resolve file extensions by default")
				.containsExactly(MediaType.ALL);

		this.servletRequest.setRequestURI("/flower?format=gif");
		this.servletRequest.addParameter("format", "gif");

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should not resolve request parameters by default")
				.isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertThat(manager.resolveMediaTypes(this.webRequest))
				.as("Should resolve Accept header by default")
				.containsExactly(MediaType.IMAGE_GIF);
	}

	@Test
	public void addMediaTypes() throws Exception {
		this.configurer.favorParameter(true);
		this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("format", "json");
		assertThat(manager.resolveMediaTypes(this.webRequest)).containsExactly(MediaType.APPLICATION_JSON);
	}

	@Test
	public void favorParameter() throws Exception {
		this.configurer.favorParameter(true);
		this.configurer.parameterName("f");
		this.configurer.mediaTypes(Collections.singletonMap("json", MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addParameter("f", "json");

		assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void ignoreAcceptHeader() throws Exception {
		this.configurer.ignoreAcceptHeader(true);
		this.configurer.favorParameter(true);
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		this.servletRequest.setRequestURI("/flower");
		this.servletRequest.addHeader("Accept", MediaType.IMAGE_GIF_VALUE);

		assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	public void setDefaultContentType() throws Exception {
		this.configurer.defaultContentType(MediaType.APPLICATION_JSON);
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void setMultipleDefaultContentTypes() throws Exception {
		this.configurer.defaultContentType(MediaType.APPLICATION_JSON, MediaType.ALL);
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		assertThat(manager.resolveMediaTypes(this.webRequest)).isEqualTo(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.ALL));
	}

	@Test
	public void setDefaultContentTypeStrategy() throws Exception {
		this.configurer.defaultContentTypeStrategy(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON));
		ContentNegotiationManager manager = this.configurer.buildContentNegotiationManager();

		assertThat(manager.resolveMediaTypes(this.webRequest).get(0)).isEqualTo(MediaType.APPLICATION_JSON);
	}

}
