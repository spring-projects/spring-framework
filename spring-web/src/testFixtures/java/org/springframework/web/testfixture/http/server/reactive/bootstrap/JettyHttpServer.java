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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;

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

		this.contextHandler = new ServletContextHandler(this.jettyServer, "", false, false);
		this.contextHandler.addServlet(servletHolder, "/");
		this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());
		this.contextHandler.start();

		ServerConnector connector = new ServerConnector(this.jettyServer);
		connector.setHost(getHost());
		connector.setPort(getPort());
		this.jettyServer.addConnector(connector);
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
			if (this.contextHandler.isRunning()) {
				this.contextHandler.stop();
			}
		}
		finally {
			try {
				if (this.jettyServer.isRunning()) {
					// Do not configure a large stop timeout. For example, setting a stop timeout
					// of 5000 adds an additional 1-2 seconds to the runtime of each test using
					// the Jetty sever, resulting in 2-4 extra minutes of overall build time.
					this.jettyServer.setStopTimeout(100);
					this.jettyServer.stop();
					this.jettyServer.destroy();
				}
			}
			catch (Exception ex) {
				// ignore
			}
		}
	}

	@Override
	protected void resetInternal() {
		try {
			if (this.jettyServer.isRunning()) {
				// Do not configure a large stop timeout. For example, setting a stop timeout
				// of 5000 adds an additional 1-2 seconds to the runtime of each test using
				// the Jetty sever, resulting in 2-4 extra minutes of overall build time.
				this.jettyServer.setStopTimeout(100);
				this.jettyServer.stop();
				this.jettyServer.destroy();
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		finally {
			this.jettyServer = null;
			this.contextHandler = null;
		}
	}

}
