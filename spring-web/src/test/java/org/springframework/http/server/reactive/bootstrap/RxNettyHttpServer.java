/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.server.reactive.bootstrap;

import io.netty.buffer.ByteBuf;

import org.springframework.http.server.reactive.RxNettyHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 */
public class RxNettyHttpServer extends AbstractHttpServer {

	private RxNettyHttpHandlerAdapter rxNettyHandler;

	private io.reactivex.netty.protocol.http.server.HttpServer<ByteBuf, ByteBuf> rxNettyServer;


	@Override
	protected void initServer() throws Exception {
		this.rxNettyHandler = createHttpHandlerAdapter();
		this.rxNettyServer = io.reactivex.netty.protocol.http.server.HttpServer.newServer(getPort());
	}

	private RxNettyHttpHandlerAdapter createHttpHandlerAdapter() {
		return (getHttpHandlerMap() != null ?
				new RxNettyHttpHandlerAdapter(getHttpHandlerMap()) :
				new RxNettyHttpHandlerAdapter(getHttpHandler()));
	}


	@Override
	protected void startInternal() {
		this.rxNettyServer.start(this.rxNettyHandler);
		setPort(this.rxNettyServer.getServerPort());
	}

	@Override
	protected void stopInternal() {
		this.rxNettyServer.shutdown();
	}

	@Override
	protected void resetInternal() {
		this.rxNettyServer = null;
		this.rxNettyHandler = null;
	}

}
