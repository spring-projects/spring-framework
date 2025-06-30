/*
 * Copyright 2002-present the original author or authors.
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

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class TomcatHttpServer extends AbstractHttpServer {

	private final String baseDir;

	private final Class<?> wsListener;

	private String contextPath = "";

	private String servletMapping = "/";

	private Tomcat tomcatServer;


	/**
	 * Create a new Tomcat HTTP server using the {@code java.io.tmpdir} JVM
	 * system property as the {@code baseDir}.
	 * @since 5.2
	 */
	public TomcatHttpServer() {
		this(new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
	}

	public TomcatHttpServer(String baseDir) {
		this(baseDir, null);
	}

	public TomcatHttpServer(String baseDir, Class<?> wsListener) {
		Assert.notNull(baseDir, "Base dir must not be null");
		this.baseDir = baseDir;
		this.wsListener = wsListener;
	}


	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public void setServletMapping(String servletMapping) {
		this.servletMapping = servletMapping;
	}


	@Override
	protected void initServer() throws Exception {
		this.tomcatServer = new Tomcat();
		this.tomcatServer.setBaseDir(baseDir);
		this.tomcatServer.setHostname(getHost());
		this.tomcatServer.setPort(getPort());

		ServletHttpHandlerAdapter servlet = initServletAdapter();

		File base = new File(System.getProperty("java.io.tmpdir"));
		Context rootContext = tomcatServer.addContext(this.contextPath, base.getAbsolutePath());
		Tomcat.addServlet(rootContext, "httpHandlerServlet", servlet).setAsyncSupported(true);
		rootContext.addServletMappingDecoded(this.servletMapping, "httpHandlerServlet");
		if (wsListener != null) {
			rootContext.addApplicationListener(wsListener.getName());
		}
	}

	private ServletHttpHandlerAdapter initServletAdapter() {
		return new TomcatHttpHandlerAdapter(resolveHttpHandler());
	}


	@Override
	protected void startInternal() throws LifecycleException {
		this.tomcatServer.start();
		setPort(this.tomcatServer.getConnector().getLocalPort());
	}

	@Override
	protected void stopInternal() throws Exception {
		this.tomcatServer.stop();
		this.tomcatServer.destroy();
	}

	@Override
	protected void resetInternal() {
		this.tomcatServer = null;
	}

}
