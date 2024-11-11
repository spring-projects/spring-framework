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
import java.io.OutputStream;
import java.nio.ByteBuffer;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.HttpOutput;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;

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

	public JettyHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}


	@Override
	protected ServletServerHttpResponse createResponse(HttpServletResponse response,
			AsyncContext context, ServletServerHttpRequest request) throws IOException {

		return new Jetty12ServerHttpResponse(
				response, context, getDataBufferFactory(), getBufferSize(), request);
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
			if (output instanceof HttpOutput httpOutput) {
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
