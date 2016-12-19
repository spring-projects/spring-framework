/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.http.server.reactive.bootstrap;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class TomcatHttpServer extends HttpServerSupport implements HttpServer, InitializingBean {

	private Tomcat tomcatServer;

	private boolean running;

	private String baseDir;

	private Class<?> wsListener;


	public TomcatHttpServer() {
	}

	public TomcatHttpServer(String baseDir) {
		this.baseDir = baseDir;
	}

	public TomcatHttpServer(String baseDir, Class<?> wsListener) {
		this.baseDir = baseDir;
		this.wsListener = wsListener;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		this.tomcatServer = new Tomcat();
		if (this.baseDir != null) {
			this.tomcatServer.setBaseDir(baseDir);
		}
		this.tomcatServer.setHostname(getHost());
		this.tomcatServer.setPort(getPort());

		ServletHttpHandlerAdapter servlet = initServletHttpHandlerAdapter();

		File base = new File(System.getProperty("java.io.tmpdir"));
		Context rootContext = tomcatServer.addContext("", base.getAbsolutePath());
		Tomcat.addServlet(rootContext, "httpHandlerServlet", servlet);
		rootContext.addServletMappingDecoded("/", "httpHandlerServlet");
		if (wsListener != null) {
			rootContext.addApplicationListener(wsListener.getName());
		}
	}

	private ServletHttpHandlerAdapter initServletHttpHandlerAdapter() {
		if (getHttpHandlerMap() != null) {
			return new ServletHttpHandlerAdapter(getHttpHandlerMap());
		}
		else {
			Assert.notNull(getHttpHandler());
			return new ServletHttpHandlerAdapter(getHttpHandler());
		}
	}


	@Override
	public void start() {
		if (!this.running) {
			try {
				this.running = true;
				this.tomcatServer.start();
			}
			catch (LifecycleException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	public void stop() {
		if (this.running) {
			try {
				this.running = false;
				this.tomcatServer.stop();
				this.tomcatServer.destroy();
			}
			catch (LifecycleException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
