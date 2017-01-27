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

import org.jetbrains.annotations.NotNull;
import reactor.core.Loopback;
import reactor.ipc.netty.NettyContext;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpServer extends AbstractHttpServer implements Loopback {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.ipc.netty.http.server.HttpServer reactorServer;

	private AtomicReference<NettyContext> nettyContext = new AtomicReference<>();


	@Override
	protected void initServer() throws Exception {
		this.reactorHandler = createHttpHandlerAdapter();
		this.reactorServer = reactor.ipc.netty.http.server.HttpServer.create(getHost(), getPort());
	}

	@NotNull
	private ReactorHttpHandlerAdapter createHttpHandlerAdapter() {
		return getHttpHandlerMap() != null ?
				new ReactorHttpHandlerAdapter(getHttpHandlerMap()) :
				new ReactorHttpHandlerAdapter(getHttpHandler());
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
	protected void startInternal() {
		NettyContext nettyContext = this.reactorServer.newHandler(this.reactorHandler).block();
		this.nettyContext.set(nettyContext);
	}

	@Override
	protected void stopInternal() {
		this.nettyContext.get().dispose();
	}

	@Override
	protected void reset() {
		super.reset();
		this.reactorServer = null;
		this.reactorHandler = null;
		this.nettyContext.set(null);
	}

}
