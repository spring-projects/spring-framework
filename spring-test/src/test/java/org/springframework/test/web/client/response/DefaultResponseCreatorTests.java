/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.web.client.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for the {@link DefaultResponseCreator} factory methods.
 *
 * @author Ashley Scopes
 */
class DefaultResponseCreatorTests {
	@ParameterizedTest(name = "expect status to be set [{0}]")
	@ValueSource(ints = {200, 401, 429})
	void expectStatus(int statusValue) throws IOException {
		HttpStatus status = HttpStatus.valueOf(statusValue);
		ClientHttpResponse response = createResponse(new DefaultResponseCreator(status));
		assertThat(response.getStatusCode()).isEqualTo(status);
	}

	@Test
	void setBodyFromString() throws IOException {
		// Use unicode codepoint for "thinking" emoji to help verify correct encoding is used internally.
		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.body("hello, world! \uD83E\uDD14"));

		assertThat(IOUtils.toByteArray(response.getBody()))
				.isEqualTo("hello, world! \uD83E\uDD14".getBytes(StandardCharsets.UTF_8));
	}

	@ParameterizedTest(name = "setBodyFromStringWithCharset [{0}]")
	@ValueSource(strings = {"Cp1047", "UTF-8", "UTF-16", "US-ASCII", "ISO-8859-1"})
	void setBodyFromStringWithCharset(String charset) throws IOException {

		assumeThat(Charset.isSupported(charset))
				.overridingErrorMessage("charset %s is not supported by this JVM", charset)
				.isTrue();

		Charset charsetObj = Charset.forName(charset);

		String content = "hello! €½$~@><·─";

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.body(content, charsetObj));

		ByteBuffer expectBuff = charsetObj.encode(content);
		byte[] expect = new byte[expectBuff.remaining()];
		expectBuff.get(expect);

		assertThat(IOUtils.toByteArray(response.getBody())).isEqualTo(expect);
	}

	@Test
	void setBodyFromByteArray() throws IOException {
		byte[] body = { 0, 9, 18, 27, 36, 45, 54, 63, 72, 81, 90 };
		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK).body(body));
		assertThat(IOUtils.toByteArray(response.getBody())).isEqualTo(body);
	}

	@Test
	void setBodyFromResource() throws IOException {
		byte[] resourceContent = {7, 14, 21, 28, 35};

		Resource resource = mock(Resource.class);
		given(resource.getInputStream()).willReturn(new ByteArrayInputStream(resourceContent));

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK).body(resource));

		then(resource).should().getInputStream();

		assertThat(IOUtils.toByteArray(response.getBody())).isEqualTo(resourceContent);
	}

	@Test
	void setContentType() throws IOException {
		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.contentType(MediaType.APPLICATION_JSON));

		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
	}

	@Test
	void setLocation() throws IOException {
		URI uri = UriComponentsBuilder
				.fromUriString("https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html")
				.build()
				.toUri();

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK).location(uri));
		assertThat(response.getHeaders().getLocation()).isEqualTo(uri);
	}

	@Test
	void setHeader() throws IOException {

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.header("foo", "bar")
				.header("baz", "bork")
				.headers("lorem", "ipsum", "dolor", "sit", "amet"));

		HttpHeaders headers = response.getHeaders();
		assertThat(headers.get("foo")).isNotNull().isEqualTo(Collections.singletonList("bar"));
		assertThat(headers.get("baz")).isNotNull().isEqualTo(Collections.singletonList("bork"));
		assertThat(headers.get("lorem")).isNotNull().isEqualTo(Arrays.asList("ipsum", "dolor", "sit", "amet"));
	}

	@Test
	void setHeaders() throws IOException {

		HttpHeaders firstHeaders = new HttpHeaders();
		firstHeaders.setContentType(MediaType.APPLICATION_JSON);
		firstHeaders.setOrigin("https://github.com");

		HttpHeaders secondHeaders = new HttpHeaders();
		secondHeaders.setAllow(Collections.singleton(HttpMethod.PUT));

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.headers(firstHeaders)
				.headers(secondHeaders));

		HttpHeaders responseHeaders = response.getHeaders();

		assertThat(responseHeaders.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(responseHeaders.getOrigin()).isEqualTo("https://github.com");
		assertThat(responseHeaders.getAllow()).isEqualTo(Collections.singleton(HttpMethod.PUT));
	}

	@Test
	void setCookie() throws IOException {
		ResponseCookie firstCookie = ResponseCookie.from("user-id", "1234").build();
		ResponseCookie secondCookie = ResponseCookie.from("group-id", "5432").build();
		ResponseCookie thirdCookie = ResponseCookie.from("cookie-cookie", "cookies").build();
		ResponseCookie fourthCookie = ResponseCookie.from("foobar", "bazbork").build();

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.cookie(firstCookie)
				.cookie(secondCookie)
				.cookies(thirdCookie, fourthCookie));

		HttpHeaders responseHeaders = response.getHeaders();

		assertThat(responseHeaders.get(HttpHeaders.SET_COOKIE))
				.isNotNull()
				.containsExactly(
						firstCookie.toString(),
						secondCookie.toString(),
						thirdCookie.toString(),
						fourthCookie.toString()
				);
	}

	@Test
	void setCookies() throws IOException {
		ResponseCookie firstCookie = ResponseCookie.from("user-id", "1234").build();
		ResponseCookie secondCookie = ResponseCookie.from("group-id", "5432").build();
		MultiValueMap<String, ResponseCookie> firstCookies = new LinkedMultiValueMap<>();
		firstCookies.add(firstCookie.getName(), firstCookie);
		firstCookies.add(secondCookie.getName(), secondCookie);

		ResponseCookie thirdCookie = ResponseCookie.from("cookie-cookie", "cookies").build();
		ResponseCookie fourthCookie = ResponseCookie.from("foobar", "bazbork").build();
		MultiValueMap<String, ResponseCookie> secondCookies = new LinkedMultiValueMap<>();
		firstCookies.add(thirdCookie.getName(), thirdCookie);
		firstCookies.add(fourthCookie.getName(), fourthCookie);

		ClientHttpResponse response = createResponse(new DefaultResponseCreator(HttpStatus.OK)
				.cookies(firstCookies)
				.cookies(secondCookies));

		HttpHeaders responseHeaders = response.getHeaders();

		assertThat(responseHeaders.get(HttpHeaders.SET_COOKIE))
				.isNotNull()
				.containsExactly(
						firstCookie.toString(),
						secondCookie.toString(),
						thirdCookie.toString(),
						fourthCookie.toString()
				);
	}

	private static ClientHttpResponse createResponse(DefaultResponseCreator creator) throws IOException {
		URI uri = UriComponentsBuilder.fromUriString("/foo/bar").build().toUri();
		return creator.createResponse(new MockClientHttpRequest(HttpMethod.POST, uri));
	}
}
