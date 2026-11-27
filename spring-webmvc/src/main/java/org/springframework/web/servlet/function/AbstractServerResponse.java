/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
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

	private final HttpStatusCode statusCode;

	private final HttpHeaders headers;

	private final MultiValueMap<String, Cookie> cookies;


	protected AbstractServerResponse(
			HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies) {

		this.statusCode = statusCode;
		this.headers = HttpHeaders.readOnlyHttpHeaders(HttpHeaders.copyOf(headers));
		LinkedMultiValueMap<String, Cookie> cookiesCopy = new LinkedMultiValueMap<>(cookies.size());
		cookiesCopy.addAll(cookies);
		this.cookies = CollectionUtils.unmodifiableMultiValueMap(cookiesCopy);
	}


	@Override
	public final HttpStatusCode statusCode() {
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
	public @Nullable ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response,
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
		response.setStatus(this.statusCode.value());
		writeHeaders(response);
		writeCookies(response);
	}

	private void writeHeaders(HttpServletResponse servletResponse) {
		this.headers.forEach((headerName, headerValues) -> {
			for (String headerValue : headerValues) {
				servletResponse.addHeader(headerName, headerValue);
			}
		});

		long contentLength = this.headers.getContentLength();
		if (contentLength != -1) {
			servletResponse.setContentLengthLong(contentLength);
		}
	}

	private void writeCookies(HttpServletResponse servletResponse) {
		this.cookies.values().stream()
				.flatMap(Collection::stream)
				.forEach(servletResponse::addCookie);
	}

	protected abstract @Nullable ModelAndView writeToInternal(
			HttpServletRequest request, HttpServletResponse response, Context context)
			throws Exception;

}
