/*
 * Copyright 2002-2018 the original author or authors.
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
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.catalina.connector.CoyoteInputStream;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Tomcat APIs for reading
 * from the request and writing to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @since 5.0
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 */
public class TomcatHttpHandlerAdapter extends ServletHttpHandlerAdapter {


	public TomcatHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServletServerHttpRequest createRequest(HttpServletRequest request, AsyncContext asyncContext)
			throws IOException, URISyntaxException {

		Assert.notNull(getServletPath(), "Servlet path is not initialized");
		return new TomcatServerHttpRequest(
				request, asyncContext, getServletPath(), getDataBufferFactory(), getBufferSize());
	}

	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
			AsyncContext asyncContext, ServletServerHttpRequest request) throws IOException {

		return new TomcatServerHttpResponse(
				response, asyncContext, getDataBufferFactory(), getBufferSize(), request);
	}


	private static final class TomcatServerHttpRequest extends ServletServerHttpRequest {

		private static final Field COYOTE_REQUEST_FIELD;

		private final int bufferSize;

		private final DataBufferFactory factory;

		static {
			Field field = ReflectionUtils.findField(RequestFacade.class, "request");
			Assert.state(field != null, "Incompatible Tomcat implementation");
			ReflectionUtils.makeAccessible(field);
			COYOTE_REQUEST_FIELD = field;
		}

		TomcatServerHttpRequest(HttpServletRequest request, AsyncContext context,
				String servletPath, DataBufferFactory factory, int bufferSize)
				throws IOException, URISyntaxException {

			super(createTomcatHttpHeaders(request), request, context, servletPath, factory, bufferSize);
			this.factory = factory;
			this.bufferSize = bufferSize;
		}

		private static HttpHeaders createTomcatHttpHeaders(HttpServletRequest request) {
			RequestFacade requestFacade = getRequestFacade(request);
			org.apache.catalina.connector.Request connectorRequest = (org.apache.catalina.connector.Request)
					ReflectionUtils.getField(COYOTE_REQUEST_FIELD, requestFacade);
			Assert.state(connectorRequest != null, "No Tomcat connector request");
			Request tomcatRequest = connectorRequest.getCoyoteRequest();
			TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatRequest.getMimeHeaders());
			return new HttpHeaders(headers);
		}

		private static RequestFacade getRequestFacade(HttpServletRequest request) {
			if (request instanceof RequestFacade) {
				return (RequestFacade) request;
			}
			else if (request instanceof HttpServletRequestWrapper) {
				HttpServletRequestWrapper wrapper = (HttpServletRequestWrapper) request;
				HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
				return getRequestFacade(wrappedRequest);
			}
			else {
				throw new IllegalArgumentException("Cannot convert [" + request.getClass() +
						"] to org.apache.catalina.connector.RequestFacade");
			}
		}

		@Override
		protected DataBuffer readFromInputStream() throws IOException {
			boolean release = true;
			int capacity = this.bufferSize;
			DataBuffer dataBuffer = this.factory.allocateBuffer(capacity);
			try {
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, capacity);
				ServletRequest request = getNativeRequest();
				int read = ((CoyoteInputStream) request.getInputStream()).read(byteBuffer);
				logBytesRead(read);
				if (read > 0) {
					dataBuffer.writePosition(read);
					release = false;
					return dataBuffer;
				}
				else if (read == -1) {
					return EOF_BUFFER;
				}
				else {
					return null;
				}
			}
			finally {
				if (release) {
					DataBufferUtils.release(dataBuffer);
				}
			}
		}
	}


	private static final class TomcatServerHttpResponse extends ServletServerHttpResponse {

		private static final Field COYOTE_RESPONSE_FIELD;

		static {
			Field field = ReflectionUtils.findField(ResponseFacade.class, "response");
			Assert.state(field != null, "Incompatible Tomcat implementation");
			ReflectionUtils.makeAccessible(field);
			COYOTE_RESPONSE_FIELD = field;
		}

		TomcatServerHttpResponse(HttpServletResponse response, AsyncContext context,
				DataBufferFactory factory, int bufferSize, ServletServerHttpRequest request) throws IOException {

			super(createTomcatHttpHeaders(response), response, context, factory, bufferSize, request);
		}

		private static HttpHeaders createTomcatHttpHeaders(HttpServletResponse response) {
			ResponseFacade responseFacade = getResponseFacade(response);
			org.apache.catalina.connector.Response connectorResponse = (org.apache.catalina.connector.Response)
					ReflectionUtils.getField(COYOTE_RESPONSE_FIELD, responseFacade);
			Assert.state(connectorResponse != null, "No Tomcat connector response");
			Response tomcatResponse = connectorResponse.getCoyoteResponse();
			TomcatHeadersAdapter headers = new TomcatHeadersAdapter(tomcatResponse.getMimeHeaders());
			return new HttpHeaders(headers);
		}

		private static ResponseFacade getResponseFacade(HttpServletResponse response) {
			if (response instanceof ResponseFacade) {
				return (ResponseFacade) response;
			}
			else if (response instanceof HttpServletResponseWrapper) {
				HttpServletResponseWrapper wrapper = (HttpServletResponseWrapper) response;
				HttpServletResponse wrappedResponse = (HttpServletResponse) wrapper.getResponse();
				return getResponseFacade(wrappedResponse);
			}
			else {
				throw new IllegalArgumentException("Cannot convert [" + response.getClass() +
						"] to org.apache.catalina.connector.ResponseFacade");
			}
		}

		@Override
		protected void applyHeaders() {
			HttpServletResponse response = getNativeResponse();
			MediaType contentType = null;
			try {
				contentType = getHeaders().getContentType();
			}
			catch (Exception ex) {
				String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
				response.setContentType(rawContentType);
			}
			if (response.getContentType() == null && contentType != null) {
				response.setContentType(contentType.toString());
			}
			Charset charset = (contentType != null ? contentType.getCharset() : null);
			if (response.getCharacterEncoding() == null && charset != null) {
				response.setCharacterEncoding(charset.name());
			}
			long contentLength = getHeaders().getContentLength();
			if (contentLength != -1) {
				response.setContentLengthLong(contentLength);
			}
		}

		@Override
		protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
			ByteBuffer input = dataBuffer.asByteBuffer();
			int len = input.remaining();
			ServletResponse response = getNativeResponse();
			((CoyoteOutputStream) response.getOutputStream()).write(input);
			return len;
		}
	}

}
