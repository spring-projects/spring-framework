/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.http.server;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
class ServletServerHttpRequestTests {

	private ServletServerHttpRequest request;

	private MockHttpServletRequest mockRequest;


	@BeforeEach
	void create() {
		mockRequest = new MockHttpServletRequest();
		request = new ServletServerHttpRequest(mockRequest);
	}


	@Test
	void getMethod() {
		mockRequest.setMethod("POST");
		assertThat(request.getMethod()).as("Invalid method").isEqualTo(HttpMethod.POST);
	}

	@Test
	void getUriForSimplePath() {
		URI uri = URI.create("https://example.com/path");
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
		mockRequest.setQueryString(uri.getQuery());
		assertThat(request.getURI()).isEqualTo(uri);
	}

	@Test
	void getUriWithQueryString() {
		URI uri = URI.create("https://example.com/path?query");
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getPath());
		mockRequest.setQueryString(uri.getQuery());
		assertThat(request.getURI()).isEqualTo(uri);
	}

	// gh-20960
	@ParameterizedTest(name = "{displayName}({arguments})")
	@CsvSource(delimiter='|', value = {
			"query=foo    | ?query=foo",
			"query=foo%%x | ?query=foo%25%25x"
	})
	void getUriWithMalformedQueryParam(String inputQuery, String expectedQuery) {
		mockRequest.setScheme("https");
		mockRequest.setServerPort(443);
		mockRequest.setServerName("example.com");
		mockRequest.setRequestURI("/path");
		mockRequest.setQueryString(inputQuery);
		assertThat(request.getURI()).isEqualTo(URI.create("https://example.com/path" + expectedQuery));
	}

	@Test
	void getUriWithMalformedPath() {
		mockRequest.setScheme("https");
		mockRequest.setServerPort(443);
		mockRequest.setServerName("example.com");
		mockRequest.setRequestURI("/p%th");
		assertThatIllegalStateException().isThrownBy(() -> request.getURI());
	}

	@Test  // SPR-13876
	void getUriWithEncoding() {
		URI uri = URI.create("https://example.com/%E4%B8%AD%E6%96%87" +
				"?redirect=https%3A%2F%2Fgithub.com%2Fspring-projects%2Fspring-framework");
		mockRequest.setScheme(uri.getScheme());
		mockRequest.setServerName(uri.getHost());
		mockRequest.setServerPort(uri.getPort());
		mockRequest.setRequestURI(uri.getRawPath());
		mockRequest.setQueryString(uri.getRawQuery());
		assertThat(request.getURI()).isEqualTo(uri);
	}

	@Test
	void getHeaders() {
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		String headerValue2 = "value2";
		mockRequest.addHeader(headerName, headerValue1);
		mockRequest.addHeader(headerName, headerValue2);
		mockRequest.setContentType("text/plain");
		mockRequest.setCharacterEncoding("UTF-8");

		HttpHeaders headers = request.getHeaders();
		assertThat(headers).as("No HttpHeaders returned").isNotNull();
		assertThat(headers.containsHeader(headerName)).as("Invalid headers returned").isTrue();
		List<String> headerValues = headers.get(headerName);
		assertThat(headerValues).as("No header values returned").isNotNull();
		assertThat(headerValues.size()).as("Invalid header values returned").isEqualTo(2);
		assertThat(headerValues.contains(headerValue1)).as("Invalid header values returned").isTrue();
		assertThat(headerValues.contains(headerValue2)).as("Invalid header values returned").isTrue();
		assertThat(headers.getContentType()).as("Invalid Content-Type").isEqualTo(new MediaType("text", "plain", StandardCharsets.UTF_8));
	}

	@Test
	void getHeadersWithEmptyContentTypeAndEncoding() {
		String headerName = "MyHeader";
		String headerValue1 = "value1";
		String headerValue2 = "value2";
		mockRequest.addHeader(headerName, headerValue1);
		mockRequest.addHeader(headerName, headerValue2);
		mockRequest.setContentType("");
		mockRequest.setCharacterEncoding("");

		HttpHeaders headers = request.getHeaders();
		assertThat(headers).as("No HttpHeaders returned").isNotNull();
		assertThat(headers.containsHeader(headerName)).as("Invalid headers returned").isTrue();
		List<String> headerValues = headers.get(headerName);
		assertThat(headerValues.size()).as("Invalid header values returned").isEqualTo(2);
		assertThat(headerValues.contains(headerValue1)).as("Invalid header values returned").isTrue();
		assertThat(headerValues.contains(headerValue2)).as("Invalid header values returned").isTrue();
		assertThat(headers.getContentType()).isNull();
	}

	@Test  // gh-27957
	void getHeadersWithWildcardContentType() {
		mockRequest.setContentType("*/*");
		mockRequest.removeHeader("Content-Type");
		assertThat(request.getHeaders().isEmpty()).as("Invalid content-type should not raise exception").isTrue();
	}

	@Test
	void getBody() throws IOException {
		byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
		mockRequest.setContent(content);

		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		assertThat(result).as("Invalid content returned").isEqualTo(content);
	}

	@Test  // gh-13318
	void getFormBody() throws IOException {
		mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		mockRequest.setMethod("POST");
		mockRequest.addParameter("name 1", "value 1");
		mockRequest.addParameter("name 2", "value 2+1", "value 2+2");
		mockRequest.addParameter("name 3", (String) null);

		assertFormContent("name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3");
	}

	@Test
	void getEmptyFormBody() throws IOException {
		mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		mockRequest.setMethod("POST");

		assertFormContent("");
	}

	@Test  // gh-31327
	void getFormBodyWhenQueryParamsAlsoPresent() throws IOException {
		mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		mockRequest.setMethod("POST");
		mockRequest.setQueryString("q=1");
		mockRequest.addParameter("q", "1");
		mockRequest.setContent("foo=bar".getBytes(StandardCharsets.UTF_8));
		mockRequest.addHeader("Content-Length", 7);

		assertFormContent("foo=bar");
	}

	@Test  // gh-32471
	void getFormBodyWhenNotEncodedCharactersPresent() throws IOException {
		mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
		mockRequest.setMethod("POST");
		mockRequest.addParameter("name", "Test");
		mockRequest.addParameter("lastName", "Test@er");
		mockRequest.addHeader("Content-Length", 26);

		int contentLength = assertFormContent("name=Test&lastName=Test%40er");
		assertThat(request.getHeaders().getContentLength()).isEqualTo(contentLength);
	}

	@Test  // gh-34675
	void getFormBodyWithNotUtf8Charset() throws IOException {
		String charset = "windows-1251";
		mockRequest.setContentType("application/x-www-form-urlencoded; charset=" + charset);
		mockRequest.setMethod("POST");
		mockRequest.addParameter("x", URLDecoder.decode("%e0%e0%e0", charset));

		assertFormContent("x=%E0%E0%E0");
	}

	private int assertFormContent(String expected) throws IOException {
		byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
		byte[] content = expected.getBytes(StandardCharsets.UTF_8);
		assertThat(result).as("Invalid content returned").isEqualTo(content);
		return result.length;
	}

	@Test
	void attributes() {
		request.getAttributes().put("foo", "bar");
		assertThat(mockRequest.getAttribute("foo")).isEqualTo("bar");
	}

}
