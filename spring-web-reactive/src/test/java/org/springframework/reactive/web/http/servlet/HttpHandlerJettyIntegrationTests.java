/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.reactive.web.http.servlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.springframework.reactive.web.http.AbstractHttpHandlerIntegrationTestCase;
import org.springframework.reactive.web.http.EchoHandler;

/**
 * @author Arjen Poutsma
 */
public class HttpHandlerJettyIntegrationTests
		extends AbstractHttpHandlerIntegrationTestCase {

	private static Server jettyServer;

	@BeforeClass
	public static void startServer() throws Exception {
		jettyServer = new Server();
		ServerConnector connector = new ServerConnector(jettyServer);
		connector.setPort(port);
		ServletContextHandler handler = new ServletContextHandler(jettyServer, "", false, false);
		HttpHandlerServlet servlet = new HttpHandlerServlet();
		servlet.setHandler(new EchoHandler());
		ServletHolder servletHolder = new ServletHolder(servlet);
		handler.addServlet(servletHolder, "/rx");
		jettyServer.addConnector(connector);
		jettyServer.start();
	}

	@AfterClass
	public static void stopServer() throws Exception {
		jettyServer.stop();
		jettyServer.destroy();
	}

	public static void main(String[] args) throws Exception {
		startServer();
		System.out.println("Jetty running at: " + url());

	}

}