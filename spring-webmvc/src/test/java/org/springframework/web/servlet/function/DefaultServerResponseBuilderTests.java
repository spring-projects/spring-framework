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

package org.springframework.web.servlet.function;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultServerResponseBuilderTests {

	static final ServerResponse.Context EMPTY_CONTEXT = () -> Collections.emptyList();

	@Test
	@SuppressWarnings("deprecation")
	void status() {
		ServerResponse response = ServerResponse.status(HttpStatus.CREATED).build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.rawStatusCode()).isEqualTo(201);
	}

	@Test
	void from() {
		Cookie cookie = new Cookie("foo", "bar");
		ServerResponse other = ServerResponse.ok()
				.header("foo", "bar")
				.cookie(cookie)
				.build();
		ServerResponse result = ServerResponse.from(other).build();
		assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.headers().getFirst("foo")).isEqualTo("bar");
		assertThat(result.cookies().getFirst("foo")).isEqualTo(cookie);
	}


	@Test
	void ok() {
		ServerResponse response = ServerResponse.ok().build();

		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void created() {
		URI location = URI.create("https://example.com");
		ServerResponse response = ServerResponse.created(location).build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.headers().getLocation()).isEqualTo(location);
	}

	@Test
	void accepted() {
		ServerResponse response = ServerResponse.accepted().build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
	}

	@Test
	void noContent() {
		ServerResponse response = ServerResponse.noContent().build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void seeOther() {
		URI location = URI.create("https://example.com");
		ServerResponse response = ServerResponse.seeOther(location).build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.SEE_OTHER);
		assertThat(response.headers().getLocation()).isEqualTo(location);
	}

	@Test
	void temporaryRedirect() {
		URI location = URI.create("https://example.com");
		ServerResponse response = ServerResponse.temporaryRedirect(location).build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
		assertThat(response.headers().getLocation()).isEqualTo(location);
	}

	@Test
	void permanentRedirect() {
		URI location = URI.create("https://example.com");
		ServerResponse response = ServerResponse.permanentRedirect(location).build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
		assertThat(response.headers().getLocation()).isEqualTo(location);
	}

	@Test
	void badRequest() {
		ServerResponse response = ServerResponse.badRequest().build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void notFound() {
		ServerResponse response = ServerResponse.notFound().build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void unprocessableEntity() {
		ServerResponse response = ServerResponse.unprocessableEntity().build();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@Test
	void allow() {
		ServerResponse response = ServerResponse.ok().allow(HttpMethod.GET).build();
		assertThat(response.headers().getAllow()).isEqualTo(Set.of(HttpMethod.GET));
	}

	@Test
	void contentLength() {
		ServerResponse response = ServerResponse.ok().contentLength(42).build();
		assertThat(response.headers().getContentLength()).isEqualTo(42L);
	}

	@Test
	void contentType() {
		ServerResponse response = ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).build();
		assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void eTag() {
		ServerResponse response = ServerResponse.ok().eTag("foo").build();
		assertThat(response.headers().getETag()).isEqualTo("\"foo\"");
	}

	@Test
	void lastModified() {
		ZonedDateTime now = ZonedDateTime.now();
		ServerResponse response = ServerResponse.ok().lastModified(now).build();
		long expected = now.toInstant().toEpochMilli() / 1000;
		assertThat(response.headers().getLastModified() / 1000).isEqualTo(expected);
	}

	@Test
	void cacheControlTag() {
		ServerResponse response = ServerResponse.ok().cacheControl(CacheControl.noCache()).build();
		assertThat(response.headers().getCacheControl()).isEqualTo("no-cache");
	}

	@Test
	void varyBy() {
		ServerResponse response = ServerResponse.ok().varyBy("foo").build();
		List<String> expected = Collections.singletonList("foo");
		assertThat(response.headers().getVary()).isEqualTo(expected);
	}

	@Test
	void statusCode() {
		HttpStatus statusCode = HttpStatus.ACCEPTED;
		ServerResponse response = ServerResponse.status(statusCode).build();
		assertThat(response.statusCode()).isEqualTo(statusCode);
	}

	@Test
	void headers() {
		HttpHeaders newHeaders = new HttpHeaders();
		newHeaders.set("foo", "bar");
		ServerResponse response = ServerResponse.ok()
				.headers(headers -> headers.addAll(newHeaders))
				.build();
		assertThat(response.headers()).isEqualTo(newHeaders);
	}

	@Test
	void cookies() {
		MultiValueMap<String, Cookie> newCookies = new LinkedMultiValueMap<>();
		newCookies.add("name", new Cookie("name", "value"));
		ServerResponse response = ServerResponse.ok()
				.cookies(cookies -> cookies.addAll(newCookies))
				.build();
		assertThat(response.cookies()).isEqualTo(newCookies);
	}

	@Test
	void build() throws Exception {
		Cookie cookie = new Cookie("name", "value");
		ServerResponse response = ServerResponse.status(HttpStatus.CREATED)
				.header("MyKey", "MyValue")
				.cookie(cookie)
				.build();

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();

		ModelAndView mav = response.writeTo(mockRequest, mockResponse, EMPTY_CONTEXT);
		assertThat(mav).isNull();

		assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.CREATED.value());
		assertThat(mockResponse.getHeader("MyKey")).isEqualTo("MyValue");
		assertThat(mockResponse.getCookie("name").getValue()).isEqualTo("value");
	}

	@Test
	void notModifiedEtag() throws Exception {
		String etag = "\"foo\"";
		ServerResponse response = ServerResponse.ok()
				.eTag(etag)
				.body("bar");

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);

		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(mockRequest, mockResponse, EMPTY_CONTEXT);
		assertThat(mav).isNull();

		assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}

	@Test
	void notModifiedLastModified() throws Exception {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

		ServerResponse response = ServerResponse.ok()
				.lastModified(oneMinuteBeforeNow)
				.body("bar");

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));

		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ModelAndView mav = response.writeTo(mockRequest, mockResponse, EMPTY_CONTEXT);
		assertThat(mav).isNull();

		assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
	}

	@Test
	void body() throws Exception {
		String body = "foo";
		ServerResponse response = ServerResponse.ok().body(body);

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());

		ModelAndView mav = response.writeTo(mockRequest, mockResponse, context);
		assertThat(mav).isNull();

		assertThat(mockResponse.getContentAsString()).isEqualTo(body);
	}

	@Test
	void bodyWithParameterizedTypeReference() throws Exception {
		List<String> body = new ArrayList<>();
		body.add("foo");
		body.add("bar");
		ServerResponse response = ServerResponse.ok().body(body, new ParameterizedTypeReference<List<String>>() {});

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		ServerResponse.Context context = () -> Collections.singletonList(new MappingJackson2HttpMessageConverter());

		ModelAndView mav = response.writeTo(mockRequest, mockResponse, context);
		assertThat(mav).isNull();

		assertThat(mockResponse.getContentAsString()).isEqualTo("[\"foo\",\"bar\"]");
	}

	@Test
	void bodyCompletionStage() throws Exception {
		String body = "foo";
		CompletionStage<String> completionStage = CompletableFuture.completedFuture(body);
		ServerResponse response = ServerResponse.ok().body(completionStage);

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		mockRequest.setAsyncSupported(true);

		ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());

		ModelAndView mav = response.writeTo(mockRequest, mockResponse, context);
		assertThat(mav).isNull();

		assertThat(mockResponse.getContentAsString()).isEqualTo(body);
	}

	@Test
	void bodyPublisher() throws Exception {
		String body = "foo";
		Publisher<String> publisher = Mono.just(body);
		ServerResponse response = ServerResponse.ok().body(publisher);

		MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		mockRequest.setAsyncSupported(true);

		ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());

		ModelAndView mav = response.writeTo(mockRequest, mockResponse, context);
		assertThat(mav).isNull();

		assertThat(mockResponse.getContentAsString()).isEqualTo(body);
	}

}
