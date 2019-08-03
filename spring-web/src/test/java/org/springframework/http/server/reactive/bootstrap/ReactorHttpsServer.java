/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.server.reactive.bootstrap;

import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.SslProvider.DefaultConfigurationType;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpsServer extends AbstractHttpServer {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.netty.http.server.HttpServer reactorServer;

	private AtomicReference<DisposableServer> serverRef = new AtomicReference<>();


	@Override
	protected void initServer() throws Exception {

		SelfSignedCertificate cert = new SelfSignedCertificate();
		SslContextBuilder builder = SslContextBuilder.forServer(cert.certificate(), cert.privateKey());

		this.reactorHandler = createHttpHandlerAdapter();
		this.reactorServer = reactor.netty.http.server.HttpServer.create()
			.host(getHost())
			.port(getPort())
			.secure(spec -> spec.sslContext(builder).defaultConfiguration(DefaultConfigurationType.TCP));
	}

	private ReactorHttpHandlerAdapter createHttpHandlerAdapter() {
		return new ReactorHttpHandlerAdapter(resolveHttpHandler());
	}

	@Override
	protected void startInternal() {
		DisposableServer server = this.reactorServer.handle(this.reactorHandler).bind().block();
		setPort(server.address().getPort());
		this.serverRef.set(server);
	}

	@Override
	protected void stopInternal() {
		this.serverRef.get().dispose();
	}

	@Override
	protected void resetInternal() {
		this.reactorServer = null;
		this.reactorHandler = null;
		this.serverRef.set(null);
	}

}
