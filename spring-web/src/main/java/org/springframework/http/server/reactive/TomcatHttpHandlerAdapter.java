/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.nio.ByteBuffer;
import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.CoyoteInputStream;
import org.apache.catalina.connector.CoyoteOutputStream;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;

/**
 * {@link ServletHttpHandlerAdapter} extension that uses Tomcat APIs for reading
 * from the request and writing to the response with {@link ByteBuffer}.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
@WebServlet(asyncSupported = true)
public class TomcatHttpHandlerAdapter extends ServletHttpHandlerAdapter {


	public TomcatHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServerHttpRequest createRequest(HttpServletRequest request, AsyncContext asyncContext)
			throws IOException {

		Assert.notNull(getServletPath(), "servletPath is not initialized.");
		return new TomcatServerHttpRequest(request, asyncContext, getServletPath(),
				getDataBufferFactory(), getBufferSize());
	}

	@Override
	protected ServerHttpResponse createResponse(HttpServletResponse response, AsyncContext cxt)
			throws IOException {

		return new TomcatServerHttpResponse(response, cxt, getDataBufferFactory(), getBufferSize());
	}


	private final class TomcatServerHttpRequest extends ServletServerHttpRequest {

		public TomcatServerHttpRequest(HttpServletRequest request, AsyncContext context,
				String servletPath, DataBufferFactory factory, int bufferSize) throws IOException {

			super(request, context, servletPath, factory, bufferSize);
		}

		@Override
		protected DataBuffer readFromInputStream() throws IOException {
			boolean release = true;
			int capacity = getBufferSize();
			DataBuffer dataBuffer = getDataBufferFactory().allocateBuffer(capacity);
			try {
				ByteBuffer byteBuffer = dataBuffer.asByteBuffer(0, capacity);

				ServletRequest request = getNativeRequest();
				int read = ((CoyoteInputStream) request.getInputStream()).read(byteBuffer);
				if (logger.isTraceEnabled()) {
					logger.trace("read:" + read);
				}

				if (read > 0) {
					dataBuffer.writePosition(read);
					release = false;
					return dataBuffer;
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

		public TomcatServerHttpResponse(HttpServletResponse response, AsyncContext context,
				DataBufferFactory factory, int bufferSize) throws IOException {

			super(response, context, factory, bufferSize);
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
