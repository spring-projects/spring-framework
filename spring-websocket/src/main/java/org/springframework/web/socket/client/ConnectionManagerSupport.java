/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A base class for WebSocket connection managers. Provides a declarative style of
 * connecting to a WebSocket server given a URI to connect to. The connection occurs when
 * the Spring ApplicationContext is refreshed, if the {@link #autoStartup} property is set
 * to {@code true}, or if set to {@code false}, the {@link #start()} and #stop methods can
 * be invoked manually.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class ConnectionManagerSupport implements SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());


	private final URI uri;

	private boolean autoStartup = false;

	private boolean isRunning = false;

	private int phase = Integer.MAX_VALUE;

	private final Object lifecycleMonitor = new Object();


	public ConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
		this.uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(
				uriVariables).encode().toUri();
	}


	/**
	 * Set whether to auto-connect to the remote endpoint after this connection manager
	 * has been initialized and the Spring context has been refreshed.
	 *
	 * <p>Default is "false".
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Return the value for the 'autoStartup' property. If "true", this endpoint
	 * connection manager will connect to the remote endpoint upon a
	 * ContextRefreshedEvent.
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Specify the phase in which a connection should be established to the remote
	 * endpoint and subsequently closed. The startup order proceeds from lowest to
	 * highest, and the shutdown order is the reverse of that. By default this value is
	 * Integer.MAX_VALUE meaning that this endpoint connection factory connects as late as
	 * possible and is closed as soon as possible.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * Return the phase in which this endpoint connection factory will be auto-connected
	 * and stopped.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	protected URI getUri() {
		return this.uri;
	}

	/**
	 * Return whether this ConnectionManager has been started.
	 */
	@Override
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.isRunning;
		}
	}

	/**
	 * Start the websocket connection. If already connected, the method has no impact.
	 */
	@Override
	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				startInternal();
			}
		}
	}

	protected void startInternal() {
		synchronized (lifecycleMonitor) {
			if (logger.isDebugEnabled()) {
				logger.debug("Starting " + this.getClass().getSimpleName());
			}
			this.isRunning = true;
			openConnection();
		}
	}

	protected abstract void openConnection();

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Stopping " + this.getClass().getSimpleName());
				}
				try {
					stopInternal();
				}
				catch (Throwable e) {
					logger.error("Failed to stop WebSocket connection", e);
				}
				finally {
					this.isRunning = false;
				}
			}
		}
	}

	protected void stopInternal() throws Exception {
		if (isConnected()) {
			closeConnection();
		}
	}

	protected abstract boolean isConnected();

	protected abstract void closeConnection() throws Exception;

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			this.stop();
			callback.run();
		}
	}

}
