/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.testfixture.http.server.reactive.bootstrap;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty5.DisposableServer;
import reactor.netty5.http.Http11SslContextSpec;

import org.springframework.http.server.reactive.ReactorNetty2HttpHandlerAdapter;

/**
 * This class is copied from {@link ReactorHttpsServer}.
 *
 * @author Violeta Georgieva
 * @since 6.0
 */
public class ReactorNetty2HttpsServer extends AbstractHttpServer {

	private ReactorNetty2HttpHandlerAdapter reactorHandler;

	private reactor.netty5.http.server.HttpServer reactorServer;

	private AtomicReference<DisposableServer> serverRef = new AtomicReference<>();


	@Override
	protected void initServer() throws Exception {
		SelfSignedCertificate cert = new SelfSignedCertificate();
		Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey());

		this.reactorHandler = createHttpHandlerAdapter();
		this.reactorServer = reactor.netty5.http.server.HttpServer.create()
			.host(getHost())
			.port(getPort())
			.secure(sslContextSpec -> sslContextSpec.sslContext(http11SslContextSpec));
	}

	private ReactorNetty2HttpHandlerAdapter createHttpHandlerAdapter() {
		return new ReactorNetty2HttpHandlerAdapter(resolveHttpHandler());
	}

	@Override
	protected void startInternal() {
		DisposableServer server = this.reactorServer.handle(this.reactorHandler).bind().block();
		setPort(((InetSocketAddress) server.address()).getPort());
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
