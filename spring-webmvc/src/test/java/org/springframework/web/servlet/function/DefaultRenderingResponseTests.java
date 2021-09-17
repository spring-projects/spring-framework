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

package org.springframework.web.servlet.function;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class DefaultRenderingResponseTests {

	static final ServerResponse.Context EMPTY_CONTEXT = new ServerResponse.Context() {
		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return Collections.emptyList();
		}

	};

	@Test
	public void create() throws Exception {
		String name = "foo";
		RenderingResponse result = RenderingResponse.create(name).build();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);

		assertThat(mav.getViewName()).isEqualTo(name);
	}

	@Test
	public void status() throws Exception {
		HttpStatus status = HttpStatus.I_AM_A_TEAPOT;
		RenderingResponse result = RenderingResponse.create("foo").status(status).build();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();
		assertThat(response.getStatus()).isEqualTo(status.value());
	}

	@Test
	public void headers() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");
		RenderingResponse result = RenderingResponse.create("foo")
				.headers(h -> h.addAll(headers))
				.build();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();

		assertThat(response.getHeader("foo")).isEqualTo("bar");
	}

	@Test
	public void modelAttribute() throws Exception {
		RenderingResponse result = RenderingResponse.create("foo")
				.modelAttribute("foo", "bar").build();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();

		assertThat(mav.getModel().get("foo")).isEqualTo("bar");
	}


	@Test
	public void modelAttributeConventions() throws Exception {
		RenderingResponse result = RenderingResponse.create("foo")
				.modelAttribute("bar").build();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();
		assertThat(mav.getModel().get("string")).isEqualTo("bar");
	}

	@Test
	public void modelAttributes() throws Exception {
		Map<String, String> model = Collections.singletonMap("foo", "bar");
		RenderingResponse result = RenderingResponse.create("foo")
				.modelAttributes(model).build();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();
		assertThat(mav.getModel().get("foo")).isEqualTo("bar");
	}

	@Test
	public void modelAttributesConventions() throws Exception {
		RenderingResponse result = RenderingResponse.create("foo")
				.modelAttributes("bar").build();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();
		assertThat(mav.getModel().get("string")).isEqualTo("bar");
	}

	@Test
	public void cookies() throws Exception {
		MultiValueMap<String, Cookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", new Cookie("name", "value"));
		RenderingResponse result =
				RenderingResponse.create("foo").cookies(cookies -> cookies.addAll(newCookies)).build();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNotNull();
		assertThat(response.getCookies().length).isEqualTo(1);
		assertThat(response.getCookies()[0].getName()).isEqualTo("name");
		assertThat(response.getCookies()[0].getValue()).isEqualTo("value");
	}

	@Test
	public void notModifiedEtag() throws Exception {
		String etag = "\"foo\"";
		RenderingResponse result = RenderingResponse.create("bar")
				.header(HttpHeaders.ETAG, etag)
				.build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "https://example.com");
		request.addHeader(HttpHeaders.IF_NONE_MATCH, etag);
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}


	@Test
	public void notModifiedLastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

		RenderingResponse result = RenderingResponse.create("bar")
				.header(HttpHeaders.LAST_MODIFIED, DateTimeFormatter.RFC_1123_DATE_TIME.format(oneMinuteBeforeNow))
				.build();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "https://example.com");
		request.addHeader(HttpHeaders.IF_MODIFIED_SINCE,DateTimeFormatter.RFC_1123_DATE_TIME.format(now));
		MockHttpServletResponse response = new MockHttpServletResponse();

		ModelAndView mav = result.writeTo(request, response, EMPTY_CONTEXT);
		assertThat(mav).isNull();
		assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}


}
