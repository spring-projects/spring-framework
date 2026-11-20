/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.nio.charset.Charset;
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
import org.springframework.http.MediaType;
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
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
		this.cookies = CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>(cookies));
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

		// HttpServletResponse exposes some headers as properties: we should include those if not already present
		if (servletResponse.getContentType() == null && this.headers.containsHeader(HttpHeaders.CONTENT_TYPE)) {
			servletResponse.setContentType(this.headers.getFirst(HttpHeaders.CONTENT_TYPE));
		}
		if (servletResponse.getCharacterEncoding() == null && this.headers.containsHeader(HttpHeaders.CONTENT_TYPE)) {
			try {
				// Lazy parsing into MediaType
				MediaType contentType = this.headers.getContentType();
				if (contentType != null) {
					Charset charset = contentType.getCharset();
					if (charset != null) {
						servletResponse.setCharacterEncoding(charset);
					}
				}
			}
			catch (Exception ex) {
				// Leave character encoding unspecified
			}
		}
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
