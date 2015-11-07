/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.web.http.undertow;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.reactive.web.http.HttpServer;
import org.springframework.reactive.web.http.HttpServerSupport;
import org.springframework.util.Assert;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;

/**
 * @author Marek Hawrylczak
 */
public class UndertowHttpServer extends HttpServerSupport
		implements InitializingBean, HttpServer {

	private Undertow undertowServer;

	private boolean running;

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(getHttpHandler());

		HttpHandler handler = new RequestHandlerAdapter(getHttpHandler());

		this.undertowServer = Undertow.builder()
				.addHttpListener(getPort() != -1 ? getPort() : 8080, "localhost")
				.setHandler(handler)
				.build();
	}

	@Override
	public void start() {
		if (!running) {
			this.undertowServer.start();
			running = true;
		}

	}

	@Override
	public void stop() {
		if (running) {
			this.undertowServer.stop();
			running = false;
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}
}
