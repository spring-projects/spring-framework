/*
 * Copyright 2002-2013 the original author or authors.
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
 * Burlap service endpoint, accessible via a Burlap proxy.
 * Designed for Sun's JRE 1.6 HTTP server, implementing the
 * {@link com.sun.net.httpserver.HttpHandler} interface.
 *
 * <p>Burlap is a slim, XML-based RPC protocol.
 * For information on Burlap, see the
 * <a href="http://www.caucho.com/burlap">Burlap website</a>.
 * This exporter requires Burlap 3.x.
 *
 * <p>Note: Burlap services exported with this class can be accessed by
 * any Burlap client, as there isn't any special handling involved.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see org.springframework.remoting.caucho.BurlapClientInterceptor
 * @see org.springframework.remoting.caucho.BurlapProxyFactoryBean
 * @see SimpleHessianServiceExporter
 * @see org.springframework.remoting.httpinvoker.SimpleHttpInvokerServiceExporter
 * @deprecated as of Spring 4.0, since Burlap hasn't evolved in years
 * and is effectively retired (in contrast to its sibling Hessian)
 */
@Deprecated
public class SimpleBurlapServiceExporter extends BurlapExporter implements HttpHandler {

	/**
	 * Processes the incoming Burlap request and creates a Burlap response.
	 */
	@Override
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
			logger.error("Burlap skeleton invocation failed", ex);
		}

		exchange.sendResponseHeaders(200, output.size());
		FileCopyUtils.copy(output.toByteArray(), exchange.getResponseBody());
	}

}
