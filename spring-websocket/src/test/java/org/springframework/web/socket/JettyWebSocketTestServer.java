/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import org.eclipse.jetty.ee11.servlet.FilterHolder;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;

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
		this.contextHandler.addServletContainerInitializer(new JettyWebSocketServletContainerInitializer());
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
