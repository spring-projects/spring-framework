/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture for HeaderContentNegotiationStrategy tests.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class HeaderContentNegotiationStrategyTests {

	private final HeaderContentNegotiationStrategy strategy = new HeaderContentNegotiationStrategy();

	private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();

	private final NativeWebRequest webRequest = new ServletWebRequest(this.servletRequest);


	@Test
	public void resolveMediaTypes() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).hasSize(4);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
		assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
		assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
		assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
	}

	@Test  // SPR-14506
	public void resolveMediaTypesFromMultipleHeaderValues() throws Exception {
		this.servletRequest.addHeader("Accept", "text/plain; q=0.5, text/html");
		this.servletRequest.addHeader("Accept", "text/x-dvi; q=0.8, text/x-c");
		List<MediaType> mediaTypes = this.strategy.resolveMediaTypes(this.webRequest);

		assertThat(mediaTypes).hasSize(4);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
		assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
		assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
		assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
	}

	@Test
	public void resolveMediaTypesParseError() throws Exception {
		this.servletRequest.addHeader("Accept", "textplain; q=0.5");
		assertThatExceptionOfType(HttpMediaTypeNotAcceptableException.class).isThrownBy(() ->
				this.strategy.resolveMediaTypes(this.webRequest));
	}

}
