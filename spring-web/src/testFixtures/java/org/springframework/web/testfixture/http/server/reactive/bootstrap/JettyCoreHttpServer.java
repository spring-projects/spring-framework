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
import org.springframework.http.server.reactive.JettyCoreHttpHandlerAdapter;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Greg Wilkins
 */
public class JettyCoreHttpServer extends AbstractHttpServer {

	private Server jettyServer;


	@Override
	protected void initServer() {
		this.jettyServer = new Server();

		ServerConnector connector = new ServerConnector(this.jettyServer);
		connector.setHost(getHost());
		connector.setPort(getPort());
		this.jettyServer.addConnector(connector);
		this.jettyServer.setHandler(createHandlerAdapter());
	}

	private JettyCoreHttpHandlerAdapter createHandlerAdapter() {
		return new JettyCoreHttpHandlerAdapter(resolveHttpHandler());
	}

	@Override
	protected void startInternal() throws Exception {
		this.jettyServer.start();
		setPort(((ServerConnector) this.jettyServer.getConnectors()[0]).getLocalPort());
	}

	@Override
	protected void stopInternal() {
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

	@Override
	protected void resetInternal() {
		try {
			stopInternal();
		}
		finally {
			this.jettyServer = null;
		}
	}

}
