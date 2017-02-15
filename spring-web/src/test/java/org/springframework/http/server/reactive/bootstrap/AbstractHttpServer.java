/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpServer implements HttpServer {

	private String host = "0.0.0.0";

	private int port = 0;

	private HttpHandler httpHandler;

	private Map<String, HttpHandler> handlerMap;

	private boolean running;

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
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				this.running = true;
				try {
					startInternal();
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
				this.running = false;
				try {
					stopInternal();
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

	private void reset() {
		this.host = "0.0.0.0";
		this.port = 0;
		this.httpHandler = null;
		this.handlerMap = null;
		resetInternal();
	}

	protected abstract void resetInternal();

}
