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

import java.io.File;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.apache.tomcat.websocket.server.WsContextListener;

import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Tomcat based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class TomcatWebSocketTestServer implements WebSocketTestServer {

	private static final Log logger = LogFactory.getLog(TomcatWebSocketTestServer.class);

	private Tomcat tomcatServer;

	private int port;

	private Context context;


	@Override
	public void setup() {
		Connector connector = new Connector(Http11NioProtocol.class.getName());
		connector.setPort(0);

		File baseDir = createTempDir("tomcat");
		String baseDirPath = baseDir.getAbsolutePath();

		this.tomcatServer = new Tomcat();
		this.tomcatServer.setBaseDir(baseDirPath);
		this.tomcatServer.setPort(0);
		this.tomcatServer.getService().addConnector(connector);
		this.tomcatServer.setConnector(connector);
	}

	private File createTempDir(String prefix) {
		try {
			File tempFolder = File.createTempFile(prefix + ".", "." + getPort());
			tempFolder.delete();
			tempFolder.mkdir();
			tempFolder.deleteOnExit();
			return tempFolder;
		}
		catch (IOException ex) {
			throw new IllegalStateException("Unable to create temp directory", ex);
		}
	}

	@Override
	public void deployConfig(WebApplicationContext wac, Filter... filters) {
		Assert.state(this.port != -1, "setup() was never called.");
		this.context = this.tomcatServer.addContext("", System.getProperty("java.io.tmpdir"));
		this.context.addApplicationListener(WsContextListener.class.getName());
		Tomcat.addServlet(this.context, "dispatcherServlet", new DispatcherServlet(wac)).setAsyncSupported(true);
		this.context.addServletMappingDecoded("/", "dispatcherServlet");
		for (Filter filter : filters) {
			FilterDef filterDef = new FilterDef();
			filterDef.setFilterName(filter.getClass().getName());
			filterDef.setFilter(filter);
			filterDef.setAsyncSupported("true");
			this.context.addFilterDef(filterDef);
			FilterMap filterMap = new FilterMap();
			filterMap.setFilterName(filter.getClass().getName());
			filterMap.addURLPattern("/*");
			filterMap.setDispatcher("REQUEST,FORWARD,INCLUDE,ASYNC");
			this.context.addFilterMap(filterMap);
		}
	}

	@Override
	public void undeployConfig() {
		if (this.context != null) {
			this.context.removeServletMapping("/");
			this.tomcatServer.getHost().removeChild(this.context);
		}
	}

	@Override
	public void start() throws Exception {
		this.tomcatServer.start();
		this.port = this.tomcatServer.getConnector().getLocalPort();
		this.context.addLifecycleListener(event -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Event: " + event.getType());
			}
		});
	}

	@Override
	public void stop() throws Exception {
		this.tomcatServer.stop();
		this.port = 0;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public ServletContext getServletContext() {
		return this.context.getServletContext();
	}

}
