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

package org.springframework.remoting.caucho;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import org.springframework.util.FileCopyUtils;

/**
 * HTTP request handler that exports the specified service bean as
 * Hessian service endpoint, accessible via a Hessian proxy.
 * Designed for Sun's JRE 1.6 HTTP server, implementing the
 * {@link com.sun.net.httpserver.HttpHandler} interface.
 *
 * <p>Hessian is a slim, binary RPC protocol.
 * For information on Hessian, see the
 * <a href="http://www.caucho.com/hessian">Hessian website</a>.
 * <b>Note: As of Spring 3.0, this exporter requires Hessian 3.2 or above.</b>
 *
 * <p>Hessian services exported with this class can be accessed by
 * any Hessian client, as there isn't any special handling involved.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see org.springframework.remoting.caucho.HessianClientInterceptor
 * @see org.springframework.remoting.caucho.HessianProxyFactoryBean
 * @see SimpleBurlapServiceExporter
 * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter
 */
public class SimpleHessianServiceExporter extends HessianExporter implements HttpHandler {

	/**
	 * Processes the incoming Hessian request and creates a Hessian response.
	 */
	public void handle(HttpExchange exchange) throws IOException {
		if (!"POST".equals(exchange.getRequestMethod())) {
			exchange.getResponseHeaders().set("Allow", "POST");
			exchange.sendResponseHeaders(405, -1);
			return;
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
		try {
			invoke(exchange.getRequestBody(), output);
		}
		catch (Throwable ex) {
			exchange.sendResponseHeaders(500, -1);
			logger.error("Hessian skeleton invocation failed", ex);
			return;
		}

		exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_HESSIAN);
		exchange.sendResponseHeaders(200, output.size());
		FileCopyUtils.copy(output.toByteArray(), exchange.getResponseBody());
	}

}
