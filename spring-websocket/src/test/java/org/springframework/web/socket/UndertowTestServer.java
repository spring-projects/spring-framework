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

package org.springframework.web.socket;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import java.io.IOException;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import org.xnio.OptionMap;
import org.xnio.Xnio;

import static io.undertow.servlet.Servlets.*;

/**
 * Undertow-based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class UndertowTestServer implements WebSocketTestServer {

	private int port = -1;

	private Undertow server;

	private DeploymentManager manager;


	@Override
	public void setup() {
		this.port = SocketUtils.findAvailableTcpPort();
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void deployConfig(WebApplicationContext wac, Filter... filters) {
		Assert.state(this.port != -1, "setup() was never called");
		DispatcherServletInstanceFactory servletFactory = new DispatcherServletInstanceFactory(wac);
		// manually building WebSocketDeploymentInfo in order to avoid class cast exceptions
		// with tomcat's implementation when using undertow 1.1.0+
		WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
		try {
			info.setWorker(Xnio.getInstance().createWorker(OptionMap.EMPTY));
			info.setBuffers(new org.xnio.ByteBufferSlicePool(1024,1024));
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}

		DeploymentInfo servletBuilder = deployment()
				.setClassLoader(UndertowTestServer.class.getClassLoader())
				.setDeploymentName("undertow-websocket-test")
				.setContextPath("/")
				.addServlet(servlet("DispatcherServlet", DispatcherServlet.class, servletFactory).addMapping("/").setAsyncSupported(true))
				.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
		for (final Filter filter : filters) {
			String filterName = filter.getClass().getName();
			servletBuilder.addFilter(new FilterInfo(filterName, filter.getClass(), new FilterInstanceFactory(filter)).setAsyncSupported(true));
			for (DispatcherType type : DispatcherType.values()) {
				servletBuilder.addFilterUrlMapping(filterName, "/*", type);
			}
		}
		try {
			this.manager = defaultContainer().addDeployment(servletBuilder);
			this.manager.deploy();
			HttpHandler httpHandler = this.manager.start();
			this.server = Undertow.builder().addHttpListener(this.port, "localhost").setHandler(httpHandler).build();
		}
		catch (ServletException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public ServletContext getServletContext() {
		return this.manager.getDeployment().getServletContext();
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

		public DispatcherServletInstanceFactory(WebApplicationContext wac) {
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

	private static class FilterInstanceFactory implements InstanceFactory<Filter> {

		private final Filter filter;

		private FilterInstanceFactory(Filter filter) {
			this.filter = filter;
		}

		@Override
		public InstanceHandle<Filter> createInstance() throws InstantiationException {
			return new InstanceHandle<Filter>() {
				@Override
				public Filter getInstance() {
					return filter;
				}
				@Override
				public void release() {}
			};
		}
	}

}
