/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.context.embedded.tomcat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.springframework.util.Assert;
import org.springframework.web.ServletContextInitializer;
import org.springframework.web.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.web.context.embedded.EmbeddedServletContainerException;
import org.springframework.web.context.embedded.EmbeddedServletContainerFactory;

/**
 * {@link EmbeddedServletContainerFactory} that can be used to create
 * {@link TomcatEmbeddedServletContainer}s. Can be initialized using Spring's
 * {@link ServletContextInitializer}s or Tomcat {@link LifecycleListener}s.
 *
 * <p>Unless explicitly configured otherwise this factory will created containers that
 * listen for HTTP requests on port 8080.
 *
 * @author Phillip Webb
 * @since 4.0
 * @see #setPort(int)
 * @see #setContextLifecycleListeners(Collection)
 * @see TomcatEmbeddedServletContainer
 */
public class TomcatEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory {

	private File baseDirectory;

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<LifecycleListener>();

	private boolean registerDefaultServlet = true;


	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} instance.
	 */
	public TomcatEmbeddedServletContainerFactory() {
		super();
	}

	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} that listens for
	 * requests using the specified port.
	 * @param port the port to listen on
	 */
	public TomcatEmbeddedServletContainerFactory(int port) {
		super(port);
	}

	/**
	 * Create a new {@link TomcatEmbeddedServletContainerFactory} with the specified
	 * context path and port.
	 * @param contextPath root the context path
	 * @param port the port to listen on
	 */
	public TomcatEmbeddedServletContainerFactory(String contextPath, int port) {
		super(contextPath, port);
	}


	public TomcatEmbeddedServletContainer getEmbdeddedServletContainer(
			ServletContextInitializer... initializers) {
		Tomcat tomcat = new Tomcat();
		File baseDir = (this.baseDirectory != null ? this.baseDirectory : createTempDir("tomcat"));
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		tomcat.setPort(getPort());

		// bindOnInit is critical to allow server restarts
		tomcat.getConnector().setProperty("bindOnInit", "false");

		File docBase = getValidDocumentRoot();
		docBase = (docBase != null ? docBase : createTempDir("tomcat-docbase"));
		Context context = tomcat.addContext(getContextPath(), docBase.getAbsolutePath());
		if(this.registerDefaultServlet) {
			addDefaultServlet(context);
		}
		ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
		configureContext(context, initializersToUse);
		postProcessContext(context);
		return getTomcatEmbeddedServletContainer(tomcat);
	}

	private void addDefaultServlet(Context context) {
		Wrapper defaultServlet = context.createWrapper();
		defaultServlet.setName("default");
		defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
		defaultServlet.addInitParameter("debug", "0");
		defaultServlet.addInitParameter("listings", "false");
		defaultServlet.setLoadOnStartup(1);
		context.addChild(defaultServlet);
		context.addServletMapping("/", "default");
	}

	/**
	 * Configure the Tomcat {@link Context}.
	 * @param context the Tomcat context
	 * @param initializers initializers to apply
	 */
	protected void configureContext(Context context, ServletContextInitializer[] initializers) {
		context.addLifecycleListener(new ServletContextInitializerLifecycleListener(initializers));
		for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
			context.addLifecycleListener(lifecycleListener);
		}
	}

	/**
	 * Post process the Tomcat {@link Context} before it used with the Tomcat Server.
	 * Subclasses can override this method to apply additional processing to the
	 * {@link Context}.
	 * @param context the Tomcat {@link Context}
	 */
	protected void postProcessContext(Context context) {
	}

	/**
	 * Factory method called to create the {@link TomcatEmbeddedServletContainer}.
	 * Subclasses can override this method to return a different
	 * {@link TomcatEmbeddedServletContainer} or apply additional processing to the
	 * Tomcat server.
	 * @param tomcat the Tomcat server.
	 * @return a new {@link TomcatEmbeddedServletContainer} instance
	 */
	protected TomcatEmbeddedServletContainer getTomcatEmbeddedServletContainer(Tomcat tomcat) {
		return new TomcatEmbeddedServletContainer(tomcat);
	}

	private File createTempDir(String prefix) {
		try {
			File tempFolder = File.createTempFile("tomcat", String.valueOf(getPort()));
			tempFolder.delete();
			tempFolder.mkdir();
			tempFolder.deleteOnExit();
			return tempFolder;
		}
		catch (IOException ex) {
			throw new EmbeddedServletContainerException(
					"Unable to create Tomcat baseDir", ex);
		}
	}

	/**
	 * Set if the Jetty DefaultServlet should be registered. Defaults to {@code true}
	 * so that files from the {@link #setDocumentRoot(File) document root} will be
	 * served.
	 * @param registerDefaultServlet if the Jetty default servlet should be registered
	 */
	public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
		this.registerDefaultServlet = registerDefaultServlet;
	}

	/**
	 * Set the Tomcat base directory. If not specified a temporary directory will
	 * be used.
	 * @param baseDirectory the tomcat base directory
	 */
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * Set {@link LifecycleListener}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing listeners.
	 * @param contextLifecycleListeners the listeners to set
	 */
	public void setContextLifecycleListeners(
			Collection<? extends LifecycleListener> contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners = new ArrayList<LifecycleListener>(contextLifecycleListeners);
	}

	/**
	 * Returns a mutable collection of the {@link LifecycleListener}s that will be
	 * applied to the Tomcat {@link Context}.
	 * @return the contextLifecycleListeners the listeners that will be applied
	 */
	public Collection<LifecycleListener> getContextLifecycleListeners() {
		return contextLifecycleListeners;
	}

	/**
	 * Add {@link LifecycleListener}s that should be applied to the Tomcat
	 * {@link Context}.
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

}
