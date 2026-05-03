/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
import org.springframework.http.converter.multipart.Part;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @author Brian Clozel
 * @since 7.0
 */
public class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

	private static final ResolvableType MULTIVALUEMAP_TYPE = ResolvableType.forClassWithGenerics(MultiValueMap.class,
			String.class, Part.class);

	private static final MultipartHttpMessageConverter MULTIPART_CONVERTER = new MultipartHttpMessageConverter();

	private final MockMvc mockMvc;


	/**
	 * Constructor with a MockMvc instance to perform requests with.
	 */
	public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
		Assert.notNull(mockMvc, "MockMvc must not be null");
		this.mockMvc = mockMvc;
	}


	@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
		return new MockMvcClientHttpRequest(httpMethod, uri);
	}


	/**
	 * {@link ClientHttpRequest} that executes via MockMvc.
	 */
	private class MockMvcClientHttpRequest extends MockClientHttpRequest {

		MockMvcClientHttpRequest(HttpMethod httpMethod, URI uri) {
			super(httpMethod, uri);
		}

		@Override
		public ClientHttpResponse executeInternal() {
			try {
				AbstractMockHttpServletRequestBuilder<?> servletRequestBuilder =
						adaptRequest(getMethod(), getURI(), getHeaders(), getBodyAsBytes());

				MockHttpServletResponse servletResponse = MockMvcClientHttpRequestFactory.this.mockMvc
						.perform(servletRequestBuilder)
						.andReturn()
						.getResponse();

				MockClientHttpResponse clientResponse = new MockClientHttpResponse(
						getResponseBody(servletResponse),
						HttpStatusCode.valueOf(servletResponse.getStatus()));

				copyHeaders(servletResponse, clientResponse);

				return clientResponse;
			}
			catch (Exception ex) {
				byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
				return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		private AbstractMockHttpServletRequestBuilder<?> adaptRequest(
				HttpMethod httpMethod, URI uri, HttpHeaders headers, byte[] bytes) throws IOException {

			String contentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
			AbstractMockHttpServletRequestBuilder<?> requestBuilder;

			if (StringUtils.hasLength(contentType) &&
					MediaType.MULTIPART_FORM_DATA.includes(MediaType.parseMediaType(contentType))) {

				MockMultipartHttpServletRequestBuilder multipartRequestBuilder = multipart(httpMethod, uri);
				Assert.notNull(bytes, "No multipart content");
				MockHttpInputMessage inputMessage = new MockHttpInputMessage(bytes);
				inputMessage.getHeaders().putAll(headers);

				MultiValueMap<String, Part> parts = MULTIPART_CONVERTER.read(MULTIVALUEMAP_TYPE, inputMessage, null);
				for (List<Part> partValues : parts.values()) {
					for (Part part : partValues) {
						parsePart(part, multipartRequestBuilder);
					}
				}
				requestBuilder = multipartRequestBuilder;
			}
			else {
				requestBuilder = request(httpMethod, uri);
				if (!ObjectUtils.isEmpty(bytes)) {
					requestBuilder.content(bytes);
				}
			}

			requestBuilder.headers(headers);
			addCookies(headers, requestBuilder);

			return requestBuilder;
		}

		private void parsePart(Part part, MockMultipartHttpServletRequestBuilder multipartRequestBuilder) throws IOException {
			try (InputStream content = part.content()) {
				byte[] partBytes = content.readAllBytes();
				MockPart mockPart = (part instanceof FilePart filePart ?
						new MockPart(part.name(), filePart.filename(), partBytes) :
						new MockPart(part.name(), partBytes));
				mockPart.getHeaders().putAll(part.headers());
				multipartRequestBuilder.part(mockPart);
			}
		}

		private void addCookies(HttpHeaders headers, AbstractMockHttpServletRequestBuilder<?> requestBuilder) {
			List<String> values = headers.get(HttpHeaders.COOKIE);
			if (!ObjectUtils.isEmpty(values)) {
				values.stream()
						.flatMap(header -> StringUtils.commaDelimitedListToSet(header).stream())
						.map(value -> {
							String[] cookieParts = StringUtils.split(value, "=");
							Assert.isTrue(cookieParts != null && cookieParts.length == 2, "Invalid cookie: '" + value + "'");
							return new Cookie(cookieParts[0], cookieParts[1]);
						})
						.forEach(requestBuilder::cookie);
			}
		}

		private static byte[] getResponseBody(MockHttpServletResponse servletResponse) {
			byte[] body = servletResponse.getContentAsByteArray();
			if (body.length == 0) {
				String error = servletResponse.getErrorMessage();
				if (StringUtils.hasLength(error)) {
					body = error.getBytes(StandardCharsets.UTF_8);
				}
			}
			return body;
		}

		private static void copyHeaders(
				MockHttpServletResponse servletResponse, MockClientHttpResponse clientResponse) {

			servletResponse.getHeaderNames()
					.forEach(name -> servletResponse.getHeaders(name)
							.forEach(value -> clientResponse.getHeaders().add(name, value)));
		}
	}

}
