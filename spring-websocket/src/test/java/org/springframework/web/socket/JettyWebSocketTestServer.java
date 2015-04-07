/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.socket;

import java.util.EnumSet;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Jetty based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketTestServer implements WebSocketTestServer {

	private Server jettyServer;

	private int port = -1;


	@Override
	public void setup() {
		this.port = SocketUtils.findAvailableTcpPort();
		this.jettyServer = new Server(this.port);
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void deployConfig(WebApplicationContext cxt, Filter... filters) {
		Assert.state(this.port != -1, "setup() was never called.");
		ServletContextHandler contextHandler = new ServletContextHandler();
		ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(cxt));
		contextHandler.addServlet(servletHolder, "/");
		for (Filter filter : filters) {
			contextHandler.addFilter(new FilterHolder(filter), "/*", getDispatcherTypes());
		}
		this.jettyServer.setHandler(contextHandler);
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
	}

	@Override
	public void stop() throws Exception {
		if (this.jettyServer.isRunning()) {
			this.jettyServer.setStopTimeout(5000);
			this.jettyServer.stop();
		}
	}

}
