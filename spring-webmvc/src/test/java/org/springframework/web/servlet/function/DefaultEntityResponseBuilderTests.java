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

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class DefaultEntityResponseBuilderTests {

	static final ServerResponse.Context EMPTY_CONTEXT = new ServerResponse.Context() {
		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return Collections.emptyList();
		}

	};

	@Test
	public void fromObject() {
		String body = "foo";
		EntityResponse<String> response = EntityResponse.fromObject(body).build();
		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	public void fromObjectTypeReference() {
		String body = "foo";
		EntityResponse<String> response = EntityResponse.fromObject(body,
				new ParameterizedTypeReference<String>() {})
				.build();

		assertThat(response.entity()).isSameAs(body);
	}

	@Test
	public void status() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).status(HttpStatus.CREATED).build();

		assertThat(result.statusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(result.rawStatusCode()).isEqualTo(201);
	}

	@Test
	public void allow() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).allow(HttpMethod.GET).build();
		Set<HttpMethod> expected = Set.of(HttpMethod.GET);
		assertThat(result.headers().getAllow()).isEqualTo(expected);
	}

	@Test
	public void contentLength() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).contentLength(42).build();
		assertThat(result.headers().getContentLength()).isEqualTo(42);
	}

	@Test
	public void contentType() {
		String body = "foo";
		EntityResponse<String>
				result =
				EntityResponse.fromObject(body).contentType(MediaType.APPLICATION_JSON).build();

		assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	public void etag() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).eTag("foo").build();

		assertThat(result.headers().getETag()).isEqualTo("\"foo\"");
	}

	@Test
	public void lastModified() {
		ZonedDateTime now = ZonedDateTime.now();
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).lastModified(now).build();
		long expected = now.toInstant().toEpochMilli() / 1000;
		assertThat(result.headers().getLastModified() / 1000).isEqualTo(expected);
	}

	@Test
	public void cacheControlTag() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).cacheControl(CacheControl.noCache()).build();
		assertThat(result.headers().getCacheControl()).isEqualTo("no-cache");
	}

	@Test
	public void varyBy() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).varyBy("foo").build();
		List<String> expected = Collections.singletonList("foo");
		assertThat(result.headers().getVary()).isEqualTo(expected);
	}

	@Test
	public void header() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).header("foo", "bar").build();
		assertThat(result.headers().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	public void headers() {
		String body = "foo";
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");
		EntityResponse<String> result = EntityResponse.fromObject(body)
				.headers(h -> h.addAll(headers))
				.build();
		assertThat(result.headers()).isEqualTo(headers);
	}

	@Test
	public void cookie() {
		Cookie cookie = new Cookie("name", "value");
		EntityResponse<String> result =
				EntityResponse.fromObject("foo").cookie(cookie)
						.build();
		assertThat(result.cookies().get("name").contains(cookie)).isTrue();
	}

	@Test
	public void cookies() {
		MultiValueMap<String, Cookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", new Cookie("name", "value"));
		EntityResponse<String> result =
				EntityResponse.fromObject("foo").cookies(cookies -> cookies.addAll(newCookies))
						.build();
		assertThat(result.cookies()).isEqualTo(newCookies);
	}

	@Test
	public void notModifiedEtag() throws Exception {
		String etag = "\"foo\"";
		EntityResponse<String> entityResponse = EntityResponse.fromObject("bar")
				.eTag(etag)
				.build();

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);

		MockHttpServletResponse mockResponse = new MockHttpServletResponse();

		ModelAndView mav = entityResponse.writeTo(mockRequest, mockResponse, EMPTY_CONTEXT);
		assertThat(mav).isNull();

		assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}


	@Test
	public void notModifiedLastModified() throws ServletException, IOException {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

		EntityResponse<String> entityResponse = EntityResponse.fromObject("bar")
				.lastModified(oneMinuteBeforeNow)
				.build();

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));

		MockHttpServletResponse mockResponse = new MockHttpServletResponse();

		ModelAndView mav = entityResponse.writeTo(mockRequest, mockResponse, EMPTY_CONTEXT);
		assertThat(mav).isNull();

		assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}

}
