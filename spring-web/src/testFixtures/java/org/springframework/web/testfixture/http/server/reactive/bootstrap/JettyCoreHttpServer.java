/*
 * Copyright 2002-2024 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.websocket.server.ServerWebSocketContainer;

import org.springframework.http.server.reactive.JettyCoreHttpHandlerAdapter;

/**
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Greg Wilkins
 * @since 6.2
 */
public class JettyCoreHttpServer extends AbstractHttpServer {

	protected Log logger = LogFactory.getLog(getClass().getName());

	private ArrayByteBufferPool byteBufferPool;

	private Server jettyServer;

	@Override
	protected void initServer() {
		if (logger.isTraceEnabled())
			this.byteBufferPool = new ArrayByteBufferPool.Tracking();
		this.jettyServer = new Server(null, null, byteBufferPool);

		ServerConnector connector = new ServerConnector(this.jettyServer);
		connector.setHost(getHost());
		connector.setPort(getPort());
		this.jettyServer.addConnector(connector);
		this.jettyServer.setHandler(createHandlerAdapter());

		ServerWebSocketContainer.ensure(jettyServer);
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
		boolean wasRunning = this.jettyServer.isRunning();
		try {
			this.jettyServer.stop();
		}
		catch (Exception ex) {
			// ignore
		}

		// TODO remove this or make debug only
		if (wasRunning && this.byteBufferPool instanceof ArrayByteBufferPool.Tracking tracking) {
			if (!tracking.getLeaks().isEmpty()) {
				System.err.println("Leaks:\n" + tracking.dumpLeaks());
				throw new IllegalStateException("LEAKS");
			}
		}
	}

	@Override
	protected void resetInternal() {
		try {
			if (this.jettyServer.isRunning()) {
				stopInternal();
			}
			this.jettyServer.destroy();
		}
		finally {
			this.jettyServer = null;
		}
	}
}
