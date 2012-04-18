/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.request.async.AbstractDelegatingCallable;
import org.springframework.web.context.request.async.AsyncExecutionChain;
import org.springframework.web.util.WebUtils;

/**
 * {@link javax.servlet.Filter} that generates an <code>ETag</code> value based on the content on the response.
 * This ETag is compared to the <code>If-None-Match</code> header of the request. If these headers are equal,
 * the response content is not sent, but rather a <code>304 "Not Modified"</code> status instead.
 *
 * <p>Since the ETag is based on the response content, the response (or {@link org.springframework.web.servlet.View})
 * is still rendered. As such, this filter only saves bandwidth, not server performance.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ShallowEtagHeaderFilter extends OncePerRequestFilter {

	private static String HEADER_ETAG = "ETag";

	private static String HEADER_IF_NONE_MATCH = "If-None-Match";


	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		ShallowEtagResponseWrapper responseWrapper = new ShallowEtagResponseWrapper(response);

		AsyncExecutionChain chain = AsyncExecutionChain.getForCurrentRequest(request);
		chain.addDelegatingCallable(getAsyncCallable(request, response, responseWrapper));

		filterChain.doFilter(request, responseWrapper);

		if (chain.isAsyncStarted()) {
			return;
		}

		updateResponse(request, response, responseWrapper);
	}

	/**
	 * Create a Callable to use to complete processing in an async execution chain.
	 */
	private AbstractDelegatingCallable getAsyncCallable(final HttpServletRequest request,
			final HttpServletResponse response, final ShallowEtagResponseWrapper responseWrapper) {

		return new AbstractDelegatingCallable() {
			public Object call() throws Exception {
				getNextCallable().call();
				updateResponse(request, response, responseWrapper);
				return null;
			}
		};
	}

	private void updateResponse(HttpServletRequest request, HttpServletResponse response,
			ShallowEtagResponseWrapper responseWrapper) throws IOException {

		byte[] body = responseWrapper.toByteArray();
		int statusCode = responseWrapper.getStatusCode();

		if (isEligibleForEtag(request, responseWrapper, statusCode, body)) {
			String responseETag = generateETagHeaderValue(body);
			response.setHeader(HEADER_ETAG, responseETag);

			String requestETag = request.getHeader(HEADER_IF_NONE_MATCH);
			if (responseETag.equals(requestETag)) {
				if (logger.isTraceEnabled()) {
					logger.trace("ETag [" + responseETag + "] equal to If-None-Match, sending 304");
				}
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("ETag [" + responseETag + "] not equal to If-None-Match [" + requestETag +
							"], sending normal response");
				}
				copyBodyToResponse(body, response);
			}
		}
		else {
			if (logger.isTraceEnabled()) {
				logger.trace("Response with status code [" + statusCode + "] not eligible for ETag");
			}
			copyBodyToResponse(body, response);
		}
	}

	private void copyBodyToResponse(byte[] body, HttpServletResponse response) throws IOException {
		if (body.length > 0) {
			response.setContentLength(body.length);
			FileCopyUtils.copy(body, response.getOutputStream());
		}
	}

	/**
	 * Indicates whether the given request and response are eligible for ETag generation.
	 * <p>The default implementation returns {@code true} for response status codes in the {@code 2xx} series.
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @param responseStatusCode the HTTP response status code
	 * @param responseBody the response body
	 * @return {@code true} if eligible for ETag generation; {@code false} otherwise
	 */
	protected boolean isEligibleForEtag(HttpServletRequest request, HttpServletResponse response,
			int responseStatusCode, byte[] responseBody) {

		return (responseStatusCode >= 200 && responseStatusCode < 300);
	}

	/**
	 * Generate the ETag header value from the given response body byte array.
	 * <p>The default implementation generates an MD5 hash.
	 * @param bytes the response body as byte array
	 * @return the ETag header value
	 * @see org.springframework.util.DigestUtils
	 */
	protected String generateETagHeaderValue(byte[] bytes) {
		StringBuilder builder = new StringBuilder("\"0");
		DigestUtils.appendMd5DigestAsHex(bytes, builder);
		builder.append('"');
		return builder.toString();
	}


	/**
	 * {@link HttpServletRequest} wrapper that buffers all content written to the
	 * {@linkplain #getOutputStream() output stream} and {@linkplain #getWriter() writer},
	 * and allows this content to be retrieved via a {@link #toByteArray() byte array}.
	 */
	private static class ShallowEtagResponseWrapper extends HttpServletResponseWrapper {

		private final ByteArrayOutputStream content = new ByteArrayOutputStream();

		private final ServletOutputStream outputStream = new ResponseServletOutputStream();

		private PrintWriter writer;

		private int statusCode = HttpServletResponse.SC_OK;

		private ShallowEtagResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setStatus(int sc) {
			super.setStatus(sc);
			this.statusCode = sc;
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setStatus(int sc, String sm) {
			super.setStatus(sc, sm);
			this.statusCode = sc;
		}

		@Override
		public void sendError(int sc) throws IOException {
			super.sendError(sc);
			this.statusCode = sc;
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			super.sendError(sc, msg);
			this.statusCode = sc;
		}

		@Override
		public void setContentLength(int len) {
		}

		@Override
		public ServletOutputStream getOutputStream() {
			return this.outputStream;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			if (this.writer == null) {
				String characterEncoding = getCharacterEncoding();
				this.writer = (characterEncoding != null ? new ResponsePrintWriter(characterEncoding) :
						new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING));
			}
			return this.writer;
		}

		@Override
		public void resetBuffer() {
			this.content.reset();
		}

		@Override
		public void reset() {
			super.reset();
			resetBuffer();
		}

		private int getStatusCode() {
			return statusCode;
		}

		private byte[] toByteArray() {
			return this.content.toByteArray();
		}

		private class ResponseServletOutputStream extends ServletOutputStream {

			@Override
			public void write(int b) throws IOException {
				content.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				content.write(b, off, len);
			}
		}

		private class ResponsePrintWriter extends PrintWriter {

			private ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
				super(new OutputStreamWriter(content, characterEncoding));
			}

			@Override
			public void write(char buf[], int off, int len) {
				super.write(buf, off, len);
				super.flush();
			}

			@Override
			public void write(String s, int off, int len) {
				super.write(s, off, len);
				super.flush();
			}

			@Override
			public void write(int c) {
				super.write(c);
				super.flush();
			}
		}
	}

}
