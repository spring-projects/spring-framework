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

package org.springframework.web.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.Filter} that generates an {@code ETag} value based on the
 * content on the response. This ETag is compared to the {@code If-None-Match}
 * header of the request. If these headers are equal, the response content is
 * not sent, but rather a {@code 304 "Not Modified"} status instead.
 *
 * <p>Since the ETag is based on the response content, the response
 * (e.g. a {@link org.springframework.web.servlet.View}) is still rendered.
 * As such, this filter only saves bandwidth, not server performance.
 *
 * <p><b>NOTE:</b> As of Spring Framework 5.0, this filter uses request/response
 * decorators built on the Servlet 3.1 API.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {

	private static final String DIRECTIVE_NO_STORE = "no-store";

	private static final String STREAMING_ATTRIBUTE = ShallowEtagHeaderFilter.class.getName() + ".STREAMING";


	private boolean writeWeakETag = false;


	/**
	 * Set whether the ETag value written to the response should be weak, as per RFC 7232.
	 * <p>Should be configured using an {@code <init-param>} for parameter name
	 * "writeWeakETag" in the filter definition in {@code web.xml}.
	 * @since 4.3
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-2.3">RFC 7232 section 2.3</a>
	 */
	public void setWriteWeakETag(boolean writeWeakETag) {
		this.writeWeakETag = writeWeakETag;
	}

	/**
	 * Return whether the ETag value written to the response should be weak, as per RFC 7232.
	 * @since 4.3
	 */
	public boolean isWriteWeakETag() {
		return this.writeWeakETag;
	}


	/**
	 * The default value is {@code false} so that the filter may delay the generation
	 * of an ETag until the last asynchronously dispatched thread.
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		HttpServletResponse responseToUse = response;
		if (!isAsyncDispatch(request) && !(response instanceof ContentCachingResponseWrapper)) {
			responseToUse = new HttpStreamingAwareContentCachingResponseWrapper(response, request);
		}

		filterChain.doFilter(request, responseToUse);

		if (!isAsyncStarted(request) && !isContentCachingDisabled(request)) {
			updateResponse(request, responseToUse);
		}
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ContentCachingResponseWrapper responseWrapper =
				WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
		Assert.notNull(responseWrapper, "ContentCachingResponseWrapper not found");
		HttpServletResponse rawResponse = (HttpServletResponse) responseWrapper.getResponse();
		int statusCode = responseWrapper.getStatus();

		if (rawResponse.isCommitted()) {
			responseWrapper.copyBodyToResponse();
		}
		else if (isEligibleForEtag(request, responseWrapper, statusCode, responseWrapper.getContentInputStream())) {
			String responseETag = generateETagHeaderValue(responseWrapper.getContentInputStream(), this.writeWeakETag);
			rawResponse.setHeader(HttpHeaders.ETAG, responseETag);
			String requestETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
			if (requestETag != null && ("*".equals(requestETag) || compareETagHeaderValue(requestETag, responseETag))) {
				rawResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			else {
				responseWrapper.copyBodyToResponse();
			}
		}
		else {
			responseWrapper.copyBodyToResponse();
		}
	}

	/**
	 * Indicates whether the given request and response are eligible for ETag generation.
	 * <p>The default implementation returns {@code true} if all conditions match:
	 * <ul>
	 * <li>response status codes in the {@code 2xx} series</li>
	 * <li>request method is a GET</li>
	 * <li>response Cache-Control header is not set or does not contain a "no-store" directive</li>
	 * </ul>
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param responseStatusCode the HTTP response status code
	 * @param inputStream the response body
	 * @return {@code true} if eligible for ETag generation, {@code false} otherwise
	 */
	protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
			int responseStatusCode, InputStream inputStream) {

		String method = request.getMethod();
		if (responseStatusCode >= 200 && responseStatusCode < 300 && HttpMethod.GET.matches(method)) {
			String cacheControl = response.getHeader(HttpHeaders.CACHE_CONTROL);
			return (cacheControl == null || !cacheControl.contains(DIRECTIVE_NO_STORE));
		}
		return false;
	}

	/**
	 * Generate the ETag header value from the given response body byte array.
	 * <p>The default implementation generates an MD5 hash.
	 * @param inputStream the response body as an InputStream
	 * @param isWeak whether the generated ETag should be weak
	 * @return the ETag header value
	 * @see org.springframework.util.DigestUtils
	 */
	protected String generateETagHeaderValue(InputStream inputStream, boolean isWeak) throws IOException {
		// length of W/ + " + 0 + 32bits md5 hash + "
		StringBuilder builder = new StringBuilder(37);
		if (isWeak) {
			builder.append("W/");
		}
		builder.append("\"0");
		DigestUtils.appendMd5DigestAsHex(inputStream, builder);
		builder.append('"');
		return builder.toString();
	}

	private boolean compareETagHeaderValue(String requestETag, String responseETag) {
		if (requestETag.startsWith("W/")) {
			requestETag = requestETag.substring(2);
		}
		if (responseETag.startsWith("W/")) {
			responseETag = responseETag.substring(2);
		}
		return requestETag.equals(responseETag);
	}


	/**
	 * This method can be used to disable the content caching response wrapper
	 * of the ShallowEtagHeaderFilter. This can be done before the start of HTTP
	 * streaming for example where the response will be written to asynchronously
	 * and not in the context of a Servlet container thread.
	 * @since 4.2
	 */
	public static void disableContentCaching(ServletRequest request) {
		Assert.notNull(request, "ServletRequest must not be null");
		request.setAttribute(STREAMING_ATTRIBUTE, true);
	}

	private static boolean isContentCachingDisabled(HttpServletRequest request) {
		return (request.getAttribute(STREAMING_ATTRIBUTE) != null);
	}


	private static class HttpStreamingAwareContentCachingResponseWrapper extends ContentCachingResponseWrapper {

		private final HttpServletRequest request;

		public HttpStreamingAwareContentCachingResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			return (useRawResponse() ? getResponse().getOutputStream() : super.getOutputStream());
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return (useRawResponse() ? getResponse().getWriter() : super.getWriter());
		}

		private boolean useRawResponse() {
			return isContentCachingDisabled(this.request);
		}
	}

}
