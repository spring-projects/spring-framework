/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.http.server.reactive.bootstrap;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.DisposableServer;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.tcp.SslProvider.GenericSslContextSpec;

import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpsServer extends AbstractHttpServer {

	private ReactorHttpHandlerAdapter reactorHandler;

	private reactor.netty.http.server.HttpServer reactorServer;

	private AtomicReference<DisposableServer> serverRef = new AtomicReference<>();


	@SuppressWarnings("deprecation")
	@Override
	protected void initServer() throws Exception {
		SelfSignedCertificate cert = new SelfSignedCertificate();
		GenericSslContextSpec<SslContextBuilder> http11SslContextSpec =
				Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey());

		this.reactorHandler = createHttpHandlerAdapter();
		this.reactorServer = reactor.netty.http.server.HttpServer.create()
			.host(getHost())
			.port(getPort())
			.secure(sslContextSpec -> sslContextSpec.sslContext(http11SslContextSpec));
	}

	private ReactorHttpHandlerAdapter createHttpHandlerAdapter() {
		return new ReactorHttpHandlerAdapter(resolveHttpHandler());
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
