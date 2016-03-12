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

package org.springframework.http.server.reactive.boot;

import reactor.core.flow.Loopback;
import reactor.core.state.Completable;
import reactor.io.buffer.Buffer;

import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpServer extends HttpServerSupport
		implements HttpServer, Loopback, Completable {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.io.netty.http.HttpServer<Buffer, Buffer> reactorServer;

	private DataBufferAllocator allocator = new DefaultDataBufferAllocator();

	private boolean running;

	public void setAllocator(DataBufferAllocator allocator) {
		this.allocator = allocator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.notNull(getHttpHandler());
		this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandler(), allocator);

		this.reactorServer = (getPort() != -1 ? reactor.io.netty.http.HttpServer.create(getPort()) :
				reactor.io.netty.http.HttpServer.create());
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
	public boolean isStarted() {
		return running;
	}

	@Override
	public boolean isTerminated() {
		return !running;
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
