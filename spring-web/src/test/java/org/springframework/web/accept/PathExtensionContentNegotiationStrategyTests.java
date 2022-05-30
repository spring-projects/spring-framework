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

package org.springframework.web.accept;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * A test fixture for {@link PathExtensionContentNegotiationStrategy}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
@SuppressWarnings("deprecation")
class PathExtensionContentNegotiationStrategyTests {

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final NativeWebRequest webRequest = new ServletWebRequest(servletRequest);

	private PathExtensionContentNegotiationStrategy strategy = new PathExtensionContentNegotiationStrategy();


	@Test
	void resolveMediaTypesFromMapping() throws Exception {
		this.servletRequest.setRequestURI("test.html");

		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("text", "html")));

		Map<String, MediaType> mapping = Collections.singletonMap("HTML", MediaType.APPLICATION_XHTML_XML);
		this.strategy = new PathExtensionContentNegotiationStrategy(mapping);
		mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("application", "xhtml+xml")));
	}

	@Test
	void resolveMediaTypesFromMediaTypeFactory() throws Exception {
		this.servletRequest.setRequestURI("test.xls");

		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(Arrays.asList(new MediaType("application", "vnd.ms-excel")));
	}

	@Test // SPR-8678
	void getMediaTypeFilenameWithContextPath() throws Exception {
		this.servletRequest.setContextPath("/project-1.0.0.M3");
		this.servletRequest.setRequestURI("/project-1.0.0.M3/");
		assertThat(this.strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);

		this.servletRequest.setRequestURI("/project-1.0.0.M3");
		assertThat(this.strategy.resolveMediaTypes(webRequest)).as("Context path should be excluded").isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test // SPR-9390
	void getMediaTypeFilenameWithEncodedURI() throws Exception {
		this.servletRequest.setRequestURI("/quo%20vadis%3f.html");
		List<MediaType> result = this.strategy.resolveMediaTypes(webRequest);

		assertThat(result).as("Invalid content type").isEqualTo(Collections.singletonList(new MediaType("text", "html")));
	}

	@Test // SPR-10170
	void resolveMediaTypesIgnoreUnknownExtension() throws Exception {
		this.servletRequest.setRequestURI("test.foobar");

		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
	}

	@Test
	void resolveMediaTypesDoNotIgnoreUnknownExtension() {
		this.servletRequest.setRequestURI("test.foobar");

		this.strategy.setIgnoreUnknownExtensions(false);
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class)
			.isThrownBy(() -> this.strategy.resolveMediaTypes(this.webRequest));
	}

}
