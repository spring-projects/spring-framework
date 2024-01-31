/*
 * Copyright 2002-2023 the original author or authors.
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

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import org.springframework.http.server.reactive.JettyHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class JettyHttpServer extends AbstractHttpServer {

	private Server jettyServer;

	private ServletContextHandler contextHandler;


	@Override
	protected void initServer() throws Exception {

		this.jettyServer = new Server();

		ServletHttpHandlerAdapter servlet = createServletAdapter();
		ServletHolder servletHolder = new ServletHolder(servlet);
		servletHolder.setAsyncSupported(true);

		this.contextHandler = new ServletContextHandler("", false, false);
		this.contextHandler.addServlet(servletHolder, "/");
		this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());

		ServerConnector connector = new ServerConnector(this.jettyServer);
		connector.setHost(getHost());
		connector.setPort(getPort());
		this.jettyServer.addConnector(connector);
		this.jettyServer.setHandler(this.contextHandler);
	}

	private ServletHttpHandlerAdapter createServletAdapter() {
		return new JettyHttpHandlerAdapter(resolveHttpHandler());
	}

	@Override
	protected void startInternal() throws Exception {
		this.jettyServer.start();
		setPort(((ServerConnector) this.jettyServer.getConnectors()[0]).getLocalPort());
	}

	@Override
	protected void stopInternal() throws Exception {
		try {
			this.jettyServer.stop();
		}
		catch (Exception ex) {
			// ignore
		}
	}

	@Override
	protected void resetInternal() {
		try {
			if (this.jettyServer.isRunning()) {
				this.jettyServer.stop();
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			this.jettyServer.destroy();
			this.jettyServer = null;
			this.contextHandler = null;
		}
	}

}
