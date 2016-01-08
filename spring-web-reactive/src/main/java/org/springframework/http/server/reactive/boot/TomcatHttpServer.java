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

package org.springframework.http.server.reactive.boot;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.SocketUtils;


/**
 * @author Rossen Stoyanchev
 */
public class TomcatHttpServer extends HttpServerSupport implements InitializingBean, HttpServer {

	private Tomcat tomcatServer;

	private boolean running;


	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (getPort() == -1) {
			setPort(SocketUtils.findAvailableTcpPort(8080));
		}

		this.tomcatServer = new Tomcat();
		this.tomcatServer.setPort(getPort());

		Assert.notNull(getHttpHandler());
		ServletHttpHandlerAdapter servlet = new ServletHttpHandlerAdapter();
		servlet.setHandler(getHttpHandler());

		File base = new File(System.getProperty("java.io.tmpdir"));
		Context rootContext = tomcatServer.addContext("", base.getAbsolutePath());
		Tomcat.addServlet(rootContext, "httpHandlerServlet", servlet);
		rootContext.addServletMapping("/", "httpHandlerServlet");
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

}
