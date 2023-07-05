/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Jetty APIs for writing
 * to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.0
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 */
public class JettyHttpHandlerAdapter extends ServletHttpHandlerAdapter {

	private static final boolean jetty11Present = ClassUtils.isPresent(
			"org.eclipse.jetty.server.HttpOutput", JettyHttpHandlerAdapter.class.getClassLoader());

	private static final boolean jetty12Present = ClassUtils.isPresent(
			"org.eclipse.jetty.ee10.servlet.HttpOutput", JettyHttpHandlerAdapter.class.getClassLoader());


	public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext context)
			throws IOException, URISyntaxException {

		if (jetty11Present) {
			Assert.state(getServletPath() != null, "Servlet path is not initialized");
			return new Jetty11ServerHttpRequest(
					request, context, getServletPath(), getDataBufferFactory(), getBufferSize());
		}
		else {
			return super.createRequest(request, context);
		}
	}

	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
			AsyncContext context, ServletServerHttpRequest request) throws IOException {

		if (jetty11Present) {
			return new Jetty11ServerHttpResponse(
					response, context, getDataBufferFactory(), getBufferSize(), request);
		}
		else if (jetty12Present) {
			return new Jetty12ServerHttpResponse(
					response, context, getDataBufferFactory(), getBufferSize(), request);
		}
		else {
			return super.createResponse(response, context, request);
		}
	}


	private static final class Jetty11ServerHttpRequest extends ServletServerHttpRequest {

		Jetty11ServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
				String servletPath, DataBufferFactory bufferFactory, int bufferSize)
				throws IOException, URISyntaxException {

			super(createHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
		}

		private static MultiValueMap<String, String> createHeaders(HttpServletRequest servletRequest) {
			Request request = getRequest(servletRequest);
			return new JettyHeadersAdapter(HttpFields.build(request.getHttpFields()));
		}

		private static Request getRequest(HttpServletRequest request) {
			if (request instanceof Request jettyRequest) {
				return jettyRequest;
			}
			else if (request instanceof HttpServletRequestWrapper wrapper) {
				HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
				return getRequest(wrappedRequest);
			}
			else {
				throw new IllegalArgumentException("Cannot convert [" + request.getClass() +
						"] to org.eclipse.jetty.server.Request");
			}
		}
	}


	private static final class Jetty11ServerHttpResponse extends ServletServerHttpResponse {

		Jetty11ServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
				DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
				throws IOException {

			super(createHeaders(response), response, asyncContext, bufferFactory, bufferSize, request);
		}

		private static HttpHeaders createHeaders(HttpServletResponse servletResponse) {
			Response response = getResponse(servletResponse);
			return new HttpHeaders(new JettyHeadersAdapter(response.getHttpFields()));
		}

		private static Response getResponse(HttpServletResponse response) {
			if (response instanceof Response jettyResponse) {
				return jettyResponse;
			}
			else if (response instanceof HttpServletResponseWrapper wrapper) {
				HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
				return getResponse(wrappedResponse);
			}
			else {
				throw new IllegalArgumentException("Cannot convert [" + response.getClass() +
						"] to org.eclipse.jetty.server.Response");
			}
		}

		@Override
		protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
			if (getOutputStream() instanceof HttpOutput httpOutput) {
				int len = 0;
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					while (iterator.hasNext() && httpOutput.isReady()) {
						ByteBuffer byteBuffer = iterator.next();
						len += byteBuffer.remaining();
						httpOutput.write(byteBuffer);
					}
				}
				return len;
			}
			return super.writeToOutputStream(dataBuffer);
		}

		@Override
		protected void applyHeaders() {
			adaptHeaders(false);
		}
	}


	private static final class Jetty12ServerHttpResponse extends ServletServerHttpResponse {

		Jetty12ServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
				DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request)
				throws IOException {

			super(response, asyncContext, bufferFactory, bufferSize, request);
		}

		@Override
		protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
			OutputStream output = getOutputStream();
			if (output instanceof org.eclipse.jetty.ee10.servlet.HttpOutput httpOutput) {
				int len = 0;
				try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
					while (iterator.hasNext() && httpOutput.isReady()) {
						ByteBuffer byteBuffer = iterator.next();
						len += byteBuffer.remaining();
						httpOutput.write(byteBuffer);
					}
				}
				return len;
			}
			return super.writeToOutputStream(dataBuffer);
		}
	}

}
