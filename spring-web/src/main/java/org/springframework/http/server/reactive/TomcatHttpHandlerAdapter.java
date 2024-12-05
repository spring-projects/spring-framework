/*
 * Copyright 2002-2024 the original author or authors.
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

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.apache.catalina.connector.RequestFacade;
import org.apache.catalina.connector.ResponseFacade;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Tomcat APIs for reading
 * from the request and writing to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Juergen Hoeller
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

		Assert.state(getServletPath() != null, "Servlet path is not initialized");
		return new TomcatServerHttpRequest(
				request, asyncContext, getServletPath(), getDataBufferFactory(), getBufferSize());
	}

	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
			AsyncContext asyncContext, ServletServerHttpRequest request) throws IOException {

		return new TomcatServerHttpResponse(
				response, asyncContext, getDataBufferFactory(), request);
	}


	private static final class TomcatServerHttpRequest extends ServletServerHttpRequest {

		private static final Field COYOTE_REQUEST_FIELD;

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
		}

		private static MultiValueMap<String, String> createTomcatHttpHeaders(HttpServletRequest request) {
			RequestFacade requestFacade = getRequestFacade(request);
			org.apache.catalina.connector.Request connectorRequest = (org.apache.catalina.connector.Request)
					ReflectionUtils.getField(COYOTE_REQUEST_FIELD, requestFacade);
			Assert.state(connectorRequest != null, "No Tomcat connector request");
			Request tomcatRequest = connectorRequest.getCoyoteRequest();
			return new TomcatHeadersAdapter(tomcatRequest.getMimeHeaders());
		}

		private static RequestFacade getRequestFacade(HttpServletRequest request) {
			if (request instanceof RequestFacade facade) {
				return facade;
			}
			else if (request instanceof HttpServletRequestWrapper wrapper) {
				HttpServletRequest wrappedRequest = (HttpServletRequest) wrapper.getRequest();
				return getRequestFacade(wrappedRequest);
			}
			else {
				throw new IllegalArgumentException("Cannot convert [" + request.getClass() +
						"] to org.apache.catalina.connector.RequestFacade");
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
				DataBufferFactory factory, ServletServerHttpRequest request) throws IOException {

			super(createTomcatHttpHeaders(response), response, context, factory, request);
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
			if (response instanceof ResponseFacade facade) {
				return facade;
			}
			else if (response instanceof HttpServletResponseWrapper wrapper) {
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
			adaptHeaders(true);
		}

	}

}
