/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.http.server.reactive.bootstrap;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

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

		ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter(resolveHttpHandler());
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
