/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.testfixture.http.server.reactive.bootstrap;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;

/**
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpServer implements HttpServer {

	protected Log logger = LogFactory.getLog(getClass().getName());

	private String host = "0.0.0.0";

	private int port = 0;

	private HttpHandler httpHandler;

	private Map<String, HttpHandler> handlerMap;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	@Override
	public void setHost(String host) {
		this.host = host;
	}

	public String getHost() {
		return host;
	}

	@Override
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void setHandler(HttpHandler handler) {
		this.httpHandler = handler;
	}

	public HttpHandler getHttpHandler() {
		return this.httpHandler;
	}

	public void registerHttpHandler(String contextPath, HttpHandler handler) {
		if (this.handlerMap == null) {
			this.handlerMap = new LinkedHashMap<>();
		}
		this.handlerMap.put(contextPath, handler);
	}

	public Map<String, HttpHandler> getHttpHandlerMap() {
		return this.handlerMap;
	}

	protected HttpHandler resolveHttpHandler() {
		return (getHttpHandlerMap() != null ?
				new ContextPathCompositeHandler(getHttpHandlerMap()) : getHttpHandler());
	}


	// InitializingBean

	@Override
	public final void afterPropertiesSet() throws Exception {
		Assert.notNull(this.host, "Host must not be null");
		Assert.isTrue(this.port >= 0, "Port must not be a negative number");
		Assert.isTrue(this.httpHandler != null || this.handlerMap != null, "No HttpHandler configured");
		Assert.state(!this.running, "Cannot reconfigure while running");

		synchronized (this.lifecycleMonitor) {
			initServer();
		}
	}

	protected abstract void initServer() throws Exception;


	// Lifecycle

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				String serverName = getClass().getSimpleName();
				if (logger.isDebugEnabled()) {
					logger.debug("Starting " + serverName + "...");
				}
				this.running = true;
				try {
					StopWatch stopWatch = new StopWatch();
					stopWatch.start();
					startInternal();
					long millis = stopWatch.getTotalTimeMillis();
					if (logger.isDebugEnabled()) {
						logger.debug("Server started on port " + getPort() + "(" + millis + " millis).");
					}
				}
				catch (Throwable ex) {
					throw new IllegalStateException(ex);
				}
			}
		}

	}

	protected abstract void startInternal() throws Exception;

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				String serverName = getClass().getSimpleName();
				logger.debug("Stopping " + serverName + "...");
				this.running = false;
				try {
					StopWatch stopWatch = new StopWatch();
					stopWatch.start();
					stopInternal();
					logger.debug("Server stopped (" + stopWatch.getTotalTimeMillis() + " millis).");
				}
				catch (Throwable ex) {
					throw new IllegalStateException(ex);
				}
				finally {
					reset();
				}
			}
		}
	}

	protected abstract void stopInternal() throws Exception;

	@Override
	public boolean isRunning() {
		return this.running;
	}


	private void reset() {
		this.host = "0.0.0.0";
		this.port = 0;
		this.httpHandler = null;
		this.handlerMap = null;
		resetInternal();
	}

	protected abstract void resetInternal();

}
