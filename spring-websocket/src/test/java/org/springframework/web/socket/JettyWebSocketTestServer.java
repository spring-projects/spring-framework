/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.socket;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Jetty based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class JettyWebSocketTestServer implements WebSocketTestServer {

	private Server jettyServer;

	private int port;

	private ServletContextHandler contextHandler;


	@Override
	public void setup() {
		// Let server pick its own random, available port.
		this.jettyServer = new Server(0);
	}

	@Override
	public void deployConfig(WebApplicationContext wac, Filter... filters) {
		ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(wac));
		this.contextHandler = new ServletContextHandler();
		this.contextHandler.addServlet(servletHolder, "/");
		for (Filter filter : filters) {
			this.contextHandler.addFilter(new FilterHolder(filter), "/*", getDispatcherTypes());
		}
		this.jettyServer.setHandler(this.contextHandler);
	}

	private EnumSet<DispatcherType> getDispatcherTypes() {
		return EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ASYNC);
	}

	@Override
	public void undeployConfig() {
		// Stopping jetty will undeploy the servlet
	}

	@Override
	public void start() throws Exception {
		this.jettyServer.start();
		this.contextHandler.start();

		Connector[] connectors = jettyServer.getConnectors();
		NetworkConnector connector = (NetworkConnector) connectors[0];
		this.port = connector.getLocalPort();
	}

	@Override
	public void stop() throws Exception {
		try {
			if (this.contextHandler.isRunning()) {
				this.contextHandler.stop();
			}
		}
		finally {
			if (this.jettyServer.isRunning()) {
				this.jettyServer.setStopTimeout(5000);
				this.jettyServer.stop();
			}
		}
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public ServletContext getServletContext() {
		return this.contextHandler.getServletContext();
	}

}
