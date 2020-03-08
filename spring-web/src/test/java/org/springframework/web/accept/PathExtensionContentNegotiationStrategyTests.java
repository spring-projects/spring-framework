/*
 * Copyright 2002-2019 the original author or authors.
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
package org.springframework.web.accept;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A test fixture for PathExtensionContentNegotiationStrategy.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class PathExtensionContentNegotiationStrategyTests {

	private NativeWebRequest webRequest;

	private MockHttpServletRequest servletRequest;


	@BeforeEach
	public void setup() {
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(servletRequest);
	}


	@Test
	public void resolveMediaTypesFromMapping() throws Exception {

		this.servletRequest.setRequestURI("test.html");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("text", "html")));

		Map<String, MediaType> mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		strategy = new PathExtensionContentNegotiationStrategy(mapping);
		mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("application", "xhtml+xml")));
	}

	@Test
	public void resolveMediaTypesFromMediaTypeFactory() throws Exception {

		this.servletRequest.setRequestURI("test.xls");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("application", "vnd.ms-excel")));
	}

	// SPR-8678

	@Test
	public void getMediaTypeFilenameWithContextPath() throws Exception {

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();

		this.servletRequest.setContextPath("/project-1.0.0.M3");
		this.servletRequest.setRequestURI("/project-1.0.0.M3/");
		assertThat(strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

		this.servletRequest.setRequestURI("/project-1.0.0.M3");
		assertThat(strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	// SPR-9390

	@Test
	public void getMediaTypeFilenameWithEncodedURI() throws Exception {

		this.servletRequest.setRequestURI("/quo%20vadis%3f.html");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> result = strategy.resolveMediaTypes(webRequest);

		assertThat(result).as("Invalid content type").isEqualTo(Collections.singletonList(new MediaType("text", "html")));
	}

	// SPR-10170

	@Test
	public void resolveMediaTypesIgnoreUnknownExtension() throws Exception {

		this.servletRequest.setRequestURI("test.foobar");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		List<MediaType> mediaTypes = strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	public void resolveMediaTypesDoNotIgnoreUnknownExtension() throws Exception {

		this.servletRequest.setRequestURI("test.foobar");

		PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();
		strategy.setIgnoreUnknownExtensions(false);
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				strategy.resolveMediaTypes(this.webRequest));
	}

}
