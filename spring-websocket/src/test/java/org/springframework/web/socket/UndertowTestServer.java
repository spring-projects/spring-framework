/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket;

import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.websockets.jsr.ServerWebSocketContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.xnio.ByteBufferSlicePool;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

/**
 * Undertow-based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 */
public class UndertowTestServer implements WebSocketTestServer {

	private Undertow server;

	private DeploymentManager manager;

	private final int port;


	public UndertowTestServer() {
		this.port = SocketUtils.findAvailableTcpPort();
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void deployConfig(WebApplicationContext cxt) {

		DispatcherServletInstanceFactory servletFactory = new DispatcherServletInstanceFactory(cxt);

		DeploymentInfo servletBuilder = deployment()
				.setClassLoader(UndertowTestServer.class.getClassLoader())
				.setDeploymentName("underow-websocket-test")
				.setContextPath("/")
				.addServlet(servlet("DispatcherServlet", DispatcherServlet.class, servletFactory).addMapping("/"))
				.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, new WebSocketDeploymentInfo());


		this.manager = defaultContainer().addDeployment(servletBuilder);
		this.manager.deploy();

		try {
			this.server = Undertow.builder()
					.addListener(this.port, "localhost")
					.setHandler(this.manager.start()).build();
		}
		catch (ServletException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public void undeployConfig() {
		this.manager.undeploy();
	}

	@Override
	public void start() throws Exception {
		this.server.start();
	}

	@Override
	public void stop() throws Exception {
		this.server.stop();
	}


	private static class DispatcherServletInstanceFactory implements InstanceFactory<Servlet> {

		private final WebApplicationContext wac;


		private DispatcherServletInstanceFactory(WebApplicationContext wac) {
			this.wac = wac;
		}

		@Override
		public InstanceHandle<Servlet> createInstance() throws InstantiationException {
			return new InstanceHandle<Servlet>() {
				@Override
				public Servlet getInstance() {
					return new DispatcherServlet(wac);
				}
				@Override
				public void release() {
				}
			};
		}
	}
}
