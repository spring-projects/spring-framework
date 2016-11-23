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
import reactor.ipc.netty.NettyContext;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpServer extends HttpServerSupport implements HttpServer, Loopback {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.ipc.netty.http.server.HttpServer reactorServer;

	private NettyContext running;


	@Override
	public void afterPropertiesSet() throws Exception {
		if (getHttpHandlerMap() != null) {
			this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandlerMap());
		}
		else {
			Assert.notNull(getHttpHandler());
			this.reactorHandler = new ReactorHttpHandlerAdapter(getHttpHandler());
		}
		this.reactorServer = reactor.ipc.netty.http.server.HttpServer.create(getHost(),
				getPort());
	}


	@Override
	public boolean isRunning() {
		NettyContext running = this.running;
		return running != null && running.channel()
		                                 .isActive();
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
		// TODO: should be made thread-safe (compareAndSet..)
		if (this.running == null) {
			this.running = this.reactorServer.newHandler(reactorHandler).block();
		}
	}

	@Override
	public void stop() {
		NettyContext running = this.running;
		if (running != null) {
			this.running = null;
			running.dispose();
		}
	}
}
