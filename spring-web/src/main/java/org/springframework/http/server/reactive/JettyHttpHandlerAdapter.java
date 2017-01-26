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
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpOutput;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

/**
 * Adapt {@link HttpHandler} to an {@link HttpServlet} using Servlet Async
 * support and Servlet 3.1 non-blocking I/O. Use Jetty API for writing with
 * ByteBuffer.
 * 
 * @author Violeta Georgieva
 * @since 5.0
 */
@WebServlet(asyncSupported = true)
public class JettyHttpHandlerAdapter extends ServletHttpHandlerAdapter {

	public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}

	public JettyHttpHandlerAdapter(Map<String, HttpHandler> handlerMap) {
		super(handlerMap);
	}


	@Override
	protected ServerHttpResponse createServletServerHttpResponse(
			HttpServletResponse response, AsyncContext asyncContext) throws IOException {
		return new JettyServerHttpResponse(
				response, asyncContext, getDataBufferFactory(), getBufferSize());
	}


	private static final class JettyServerHttpResponse extends ServletServerHttpResponse {

		public JettyServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
				DataBufferFactory bufferFactory, int bufferSize) throws IOException {
			super(response, asyncContext, bufferFactory, bufferSize);
		}

		@Override
		protected int writeDataBuffer(DataBuffer dataBuffer) throws IOException {
			ServletOutputStream outputStream = getServletResponse().getOutputStream();
			ByteBuffer input = dataBuffer.asByteBuffer();
			int len = input.remaining();
			if (outputStream.isReady() && len > 0) {
				((HttpOutput) outputStream).write(input);
			}
			return len;
		}
	}

}