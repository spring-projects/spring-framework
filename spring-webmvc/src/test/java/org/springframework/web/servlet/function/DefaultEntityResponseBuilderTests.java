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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

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
		assertSame(body, response.entity());
	}

	@Test
	public void fromObjectTypeReference() {
		String body = "foo";
		EntityResponse<String> response = EntityResponse.fromObject(body,
				new ParameterizedTypeReference<String>() {})
				.build();

		assertSame(body, response.entity());
	}

	@Test
	public void status() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).status(HttpStatus.CREATED).build();

		assertEquals(HttpStatus.CREATED, result.statusCode());
	}

	@Test
	public void allow() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).allow(HttpMethod.GET).build();
		Set<HttpMethod> expected = EnumSet.of(HttpMethod.GET);
		assertEquals(expected, result.headers().getAllow());
	}

	@Test
	public void contentLength() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).contentLength(42).build();
		assertEquals(42, result.headers().getContentLength());
	}

	@Test
	public void contentType() {
		String body = "foo";
		EntityResponse<String>
				result =
				EntityResponse.fromObject(body).contentType(MediaType.APPLICATION_JSON).build();

		assertEquals(MediaType.APPLICATION_JSON, result.headers().getContentType());
	}

	@Test
	public void etag() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).eTag("foo").build();

		assertEquals("\"foo\"", result.headers().getETag());
	}

	@Test
	public void lastModified() {
		ZonedDateTime now = ZonedDateTime.now();
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).lastModified(now).build();
		long expected = now.toInstant().toEpochMilli() / 1000;
		assertEquals(expected, result.headers().getLastModified() / 1000);
	}

	@Test
	public void cacheControlTag() {
		String body = "foo";
		EntityResponse<String> result =
				EntityResponse.fromObject(body).cacheControl(CacheControl.noCache()).build();
		assertEquals("no-cache", result.headers().getCacheControl());
	}

	@Test
	public void varyBy() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).varyBy("foo").build();
		List<String> expected = Collections.singletonList("foo");
		assertEquals(expected, result.headers().getVary());
	}

	@Test
	public void header() {
		String body = "foo";
		EntityResponse<String> result = EntityResponse.fromObject(body).header("foo", "bar").build();
		assertEquals("bar", result.headers().getFirst("foo"));
	}

	@Test
	public void headers() {
		String body = "foo";
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");
		EntityResponse<String> result = EntityResponse.fromObject(body).headers(headers).build();
		assertEquals(headers, result.headers());
	}

	@Test
	public void cookie() {
		Cookie cookie = new Cookie("name", "value");
		EntityResponse<String> result =
				EntityResponse.fromObject("foo").cookie(cookie)
						.build();
		assertTrue(result.cookies().get("name").contains(cookie));
	}

	@Test
	public void cookies() {
		MultiValueMap<String, Cookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", new Cookie("name", "value"));
		EntityResponse<String> result =
				EntityResponse.fromObject("foo").cookies(cookies -> cookies.addAll(newCookies))
						.build();
		assertEquals(newCookies, result.cookies());
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
		assertNull(mav);

		assertEquals(HttpStatus.NOT_MODIFIED.value(), mockResponse.getStatus());
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
		assertNull(mav);

		assertEquals(HttpStatus.NOT_MODIFIED.value(), mockResponse.getStatus());
	}

}
