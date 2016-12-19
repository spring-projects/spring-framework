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

import java.util.concurrent.atomic.AtomicReference;

import reactor.core.Loopback;
import reactor.ipc.netty.NettyContext;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpServer extends HttpServerSupport implements HttpServer, Loopback {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.ipc.netty.http.server.HttpServer reactorServer;

	private AtomicReference<NettyContext> nettyContext = new AtomicReference<>();


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getHttpHandlerMap() != null) {
			this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandlerMap());
		}
		else {
			Assert.notNull(getHttpHandler());
			this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandler());
		}
		this.reactorServer = reactor.ipc.netty.http.server.HttpServer
				.create(getHost(), getPort());
	}


	@Override
	public boolean isRunning() {
		NettyContext context = this.nettyContext.get();
		return (context != null && context.channel().isActive());
	}

	@Override
	public Object connectedInput() {
		return this.reactorServer;
	}

	@Override
	public Object connectedOutput() {
		return this.reactorServer;
	}

	@Override
	public void start() {
		if (this.nettyContext.get() == null) {
			this.nettyContext.set(this.reactorServer.newHandler(reactorHandler).block());
		}
	}

	@Override
	public void stop() {
		NettyContext context = this.nettyContext.getAndSet(null);
		if (context != null) {
			context.dispose();
		}
	}
}
