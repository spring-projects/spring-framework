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

package org.springframework.test.web.servlet.playwright;

import com.microsoft.playwright.Request;
import com.microsoft.playwright.Route;
import jakarta.servlet.http.Part;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.multipart.FilePart;
import org.springframework.http.converter.multipart.FormFieldPart;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.playwright.fileupload.PlaywrightFileUpload;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code MockMvcPlaywrightHandler} delegates playwright
 * {@link Route} handling to  {@link MockMvc} instance.
 *
 * @author Alain Michel Chomnoue Nghemning
 */
public class MockMvcPlaywrightHandler implements Consumer<Route> {

	private static final Log logger = LogFactory.getLog(MockMvcPlaywrightHandler.class);

	private final MockMvc mockMvc;
	private final PlaywrightFileUpload fileUpload;

	private Charset charset = StandardCharsets.UTF_8;

	private MockMvcPlaywrightHandler(MockMvc mockMvc, PlaywrightFileUpload fileUpload) {
		this.mockMvc = mockMvc;
		this.fileUpload = fileUpload;
	}

	public static Builder builder(MockMvc mockMvc) {
		return new Builder(mockMvc);
	}

	private static String toPath(String uri) {
		return uri.startsWith("/") ? uri : "/" + uri;
	}

	private static Route.FulfillOptions fulfillOptions(
			MockHttpServletResponse response) {
		return new Route.FulfillOptions()
				.setStatus(response.getStatus())
				.setHeaders(
						response.getHeaderNames().stream()
								.collect(
										Collectors.toMap(Function.identity(), response::getHeader)))
				.setBodyBytes(response.getContentAsByteArray())
				.setContentType(response.getContentType());
	}

	private RequestBuilder requestBuilder(Request request) {
		var contentType = request.headerValue(HttpHeaders.CONTENT_TYPE);
		AbstractMockHttpServletRequestBuilder<?> builder;
		if (Strings.CI.contains(contentType, MediaType.MULTIPART_FORM_DATA_VALUE)) {
			builder = multiPartRequestBuilder(request);
		} else {
			builder = simpleRequestBuilder(request);
		}
		builder.queryParams(queryParams(request));
		if (StringUtils.isNotBlank(contentType)) {
			builder.contentType(contentType);
		}
		request.headersArray().forEach(header -> builder.header(header.name, header.value));
		return builder;
	}

	public MultiValueMap<String, String> queryParams(Request request) {
		final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		final String[] pairs = getQuery(request);
		for (String pair : pairs) {
			final int idx = pair.indexOf('=');
			final String key =
					idx > 0
							? URLDecoder.decode(pair.substring(0, idx), charset)
							: pair;
			final String value;
			value =
					idx > 0 && pair.length() > idx + 1
							? URLDecoder.decode(pair.substring(idx + 1), charset)
							: null;
			params.add(key, value);
		}
		return params;
	}

	private static String[] getQuery(Request request) {
		var query = getUrl(request).getQuery();
		return StringUtils.isBlank(query) ? new String[0] : query.split("&");
	}

	private MockHttpServletRequestBuilder simpleRequestBuilder(Request request) {
		return MockMvcRequestBuilders.request(
						HttpMethod.valueOf(request.method().toUpperCase()), getPath(request))
				.content(request.postDataBuffer());
	}

	private String getPath(Request request) {
		return URLDecoder.decode(getUrl(request).getPath(), charset);
	}

	private MockMultipartHttpServletRequestBuilder multiPartRequestBuilder(Request request) {
		var builder =
				MockMvcRequestBuilders.multipart(
						HttpMethod.valueOf(request.method()), getPath(request));
		var content = fileUpload.getContent(request);
		content.files().stream().map(MockMvcPlaywrightHandler::toMockMultipartFile).forEach(builder::file);
		if (!content.parts().isEmpty()) {
			var parts = content.parts().stream()
					.map(MockMvcPlaywrightHandler::toMockPart).toList().toArray(new Part[0]);
			builder.part(parts);
		}
		return builder;
	}

	private static  MockPart toMockPart(FormFieldPart part) {
		try(var content = part.content()) {
			return new MockPart(part.name(), content.readAllBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static MockMultipartFile toMockMultipartFile(FilePart part) {
		try {
			return new MockMultipartFile(part.name(),
					part.filename(),
					Optional.ofNullable(part.headers().getContentType())
							.map(MediaType::getType).orElse(MediaType.TEXT_PLAIN_VALUE),
					part.content());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static URL getUrl(Request request) {
		try {
			return new URI(request.url()).toURL();
		} catch (URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void accept(Route route) {
		route.fulfill(performRequest(route.request()));
	}

	private Route.FulfillOptions performRequest(Request request) {
		MockHttpServletResponse response;
		try {
			// System.out.println("Handling request " + request);
			response = performMockMvcRequest(requestBuilder(request), mockMvc);
			return fulfillOptions(response);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new Route.FulfillOptions()
					.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.setBody(e.getMessage());
		}
	}

	private MockHttpServletResponse performMockMvcRequest(
			RequestBuilder request, MockMvc mockMvc) throws Exception {
		var response = mockMvc.perform(request).andReturn().getResponse();
		return StringUtils.isBlank(response.getForwardedUrl())
				? response
				: performMockMvcRequest(
				MockMvcRequestBuilders.get(toPath(response.getForwardedUrl())), mockMvc);
	}

	public static final class Builder {
		private final MockMvc mockMvc;

		private Charset charset = StandardCharsets.UTF_8;

		private @Nullable List<HttpMessageConverter<?>> multiPartConverters;

		private @Nullable List<MediaType> multiPartSupportedMediaTypes;

		private @Nullable Charset multipartCharset;

		private int multiPartMaxInMemorySize = 256 * 1024;

		private int multipartMaxHeadersSize = 10 * 1024;

		private long maxDiskUsagePerPart = -1;

		private int maxParts = -1;

		public Builder(MockMvc mockMvc) {
			this.mockMvc = mockMvc;
		}

		public Builder charset(Charset charset) {
			this.charset = charset;
			return this;
		}

		public Builder multiPartConverters(List<HttpMessageConverter<?>> multiPartConverters) {
			this.multiPartConverters = multiPartConverters;
			return this;
		}

		public Builder multiPartSupportedMediaTypes(List<MediaType> multiPartSupportedMediaTypes) {
			this.multiPartSupportedMediaTypes = multiPartSupportedMediaTypes;
			return this;
		}

		public Builder multipartCharset(@Nullable Charset multipartCharset) {
			this.multipartCharset = multipartCharset;
			return this;
		}

		public Builder multiPartMaxInMemorySize(int multiPartMaxInMemorySize) {
			this.multiPartMaxInMemorySize = multiPartMaxInMemorySize;
			return this;
		}

		public Builder multipartMaxHeadersSize(int multipartMaxHeadersSize) {
			this.multipartMaxHeadersSize = multipartMaxHeadersSize;
			return this;
		}

		public Builder maxDiskUsagePerPart(long maxDiskUsagePerPart) {
			this.maxDiskUsagePerPart = maxDiskUsagePerPart;
			return this;
		}

		public Builder maxParts(int maxParts) {
			this.maxParts = maxParts;
			return this;
		}


		public MockMvcPlaywrightHandler build() {
			var multipartConverter = multiPartConverters==null? new MultipartHttpMessageConverter() :
					new MultipartHttpMessageConverter(multiPartConverters);
			multipartConverter.setCharset(charset);
			if (multiPartSupportedMediaTypes != null) {
				multipartConverter.setSupportedMediaTypes(multiPartSupportedMediaTypes);
			}
			if (multipartCharset != null) {
				multipartConverter.setMultipartCharset(multipartCharset);
			}
			multipartConverter.setMaxInMemorySize(multiPartMaxInMemorySize);
			multipartConverter.setMaxHeadersSize(multipartMaxHeadersSize);
			multipartConverter.setMaxDiskUsagePerPart(maxDiskUsagePerPart);
			multipartConverter.setMaxParts(maxParts);

			var fileUpload = new PlaywrightFileUpload(multipartConverter);
			var handler = new MockMvcPlaywrightHandler(mockMvc, fileUpload);
			handler.charset =charset;
			return handler;
		}
	}
}
