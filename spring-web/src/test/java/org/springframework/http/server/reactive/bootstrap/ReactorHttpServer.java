/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.server.reactive.bootstrap;

import reactor.core.Loopback;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpServer extends HttpServerSupport implements HttpServer, Loopback {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.io.netty.http.HttpServer reactorServer;

	private boolean running;


	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(getHttpHandler());
		this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandler());
		this.reactorServer = reactor.io.netty.http.HttpServer.create(getHost(), getPort());
	}


	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public Object connectedInput() {
		return reactorServer;
	}

	@Override
	public Object connectedOutput() {
		return reactorServer;
	}

	@Override
	public void start() {
		if (!this.running) {
			try {
				this.reactorServer.startAndAwait(reactorHandler);
				this.running = true;
			}
			catch (InterruptedException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			this.reactorServer.shutdown();
			this.running = false;
		}
	}
}
