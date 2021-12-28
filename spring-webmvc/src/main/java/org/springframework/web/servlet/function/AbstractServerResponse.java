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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;

/**
 * Abstract base class for {@link ServerResponse} implementations.
 *
 * @author Arjen Poutsma
 * @since 5.3.2
 */
abstract class AbstractServerResponse extends ErrorHandlingServerResponse {

	private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD);

	final int statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, Cookie> cookies;

	protected AbstractServerResponse(
			int statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies) {

		this.statusCode = statusCode;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		this.cookies =
				CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
	}

	@Override
	public final HttpStatus statusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	@Override
	public int rawStatusCode() {
		return this.statusCode;
	}

	@Override
	public final HttpHeaders headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return this.cookies;
	}

	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response,
			Context context) throws ServletException, IOException {

		try {
			writeStatusAndHeaders(response);

			long lastModified = headers().getLastModified();
			ServletWebRequest servletWebRequest = new ServletWebRequest(request, response);
			HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
			if (SAFE_METHODS.contains(httpMethod) &&
					servletWebRequest.checkNotModified(headers().getETag(), lastModified)) {
				return null;
			}
			else {
				return writeToInternal(request, response, context);
			}
		}
		catch (Throwable throwable) {
			return handleError(throwable, request, response, context);
		}
	}

	private void writeStatusAndHeaders(HttpServletResponse response) {
		response.setStatus(this.statusCode);
		writeHeaders(response);
		writeCookies(response);
	}

	private void writeHeaders(HttpServletResponse servletResponse) {
		this.headers.forEach((headerName, headerValues) -> {
			for (String headerValue : headerValues) {
				servletResponse.addHeader(headerName, headerValue);
			}
		});
		// HttpServletResponse exposes some headers as properties: we should include those if not already present
		if (servletResponse.getContentType() == null && this.headers.getContentType() != null) {
			servletResponse.setContentType(this.headers.getContentType().toString());
		}
		if (servletResponse.getCharacterEncoding() == null &&
				this.headers.getContentType() != null &&
				this.headers.getContentType().getCharset() != null) {
			servletResponse.setCharacterEncoding(this.headers.getContentType().getCharset().name());
		}
	}

	private void writeCookies(HttpServletResponse servletResponse) {
		this.cookies.values().stream()
				.flatMap(Collection::stream)
				.forEach(servletResponse::addCookie);
	}

	@Nullable
	protected abstract ModelAndView writeToInternal(
			HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException;

}
