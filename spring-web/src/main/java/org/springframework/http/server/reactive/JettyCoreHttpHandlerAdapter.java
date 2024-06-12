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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import org.springframework.core.io.buffer.JettyDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to the Jetty {@link org.eclipse.jetty.server.Handler} abstraction.
 *
 * @author Greg Wilkins
 * @author Lachlan Roberts
 * @author Arjen Poutsma
 * @since 6.2
 */
public class JettyCoreHttpHandlerAdapter extends Handler.Abstract.NonBlocking {

	private final HttpHandler httpHandler;

	private JettyDataBufferFactory dataBufferFactory = new JettyDataBufferFactory();


	public JettyCoreHttpHandlerAdapter(HttpHandler httpHandler) {
		this.httpHandler = httpHandler;
	}

	public void setDataBufferFactory(JettyDataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}


	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		this.httpHandler.handle(new JettyCoreServerHttpRequest(request, this.dataBufferFactory),
						new JettyCoreServerHttpResponse(response, this.dataBufferFactory))
				.subscribe(unused -> {}, callback::failed, callback::succeeded);
		return true;
	}

}
