/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.socket.client;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.SmartLifecycle;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Base class for a connection manager that automates the process of connecting
 * to a WebSocket server with the Spring ApplicationContext lifecycle. Connects
 * to a WebSocket server on {@link #start()} and disconnects on {@link #stop()}.
 * If {@link #setAutoStartup(boolean)} is set to {@code true} this will be done
 * automatically when the Spring {@code ApplicationContext} is refreshed.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class ConnectionManagerSupport implements SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final URI uri;

	private boolean autoStartup = false;

	private int phase = DEFAULT_PHASE;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Constructor with a URI template and variables.
	 */
	public ConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
		this.uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode().toUri();
	}

	/**
	 * Constructor with a prepared {@link URI}.
	 * @param uri the url to connect to
	 * @since 6.0.5
	 */
	public ConnectionManagerSupport(URI uri) {
		this.uri = uri;
	}


	protected URI getUri() {
		return this.uri;
	}

	/**
	 * Set whether to auto-connect to the remote endpoint after this connection manager
	 * has been initialized and the Spring context has been refreshed.
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
	 * highest, and the shutdown order is the reverse of that. By default, this value is
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


	/**
	 * Start the WebSocket connection. If already connected, the method has no impact.
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
		synchronized (this.lifecycleMonitor) {
			if (logger.isInfoEnabled()) {
				logger.info("Starting " + getClass().getSimpleName());
			}
			this.running = true;
			openConnection();
		}
	}

	@Override
	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping " + getClass().getSimpleName());
				}
				try {
					stopInternal();
				}
				catch (Throwable ex) {
					logger.error("Failed to stop WebSocket connection", ex);
				}
				finally {
					this.running = false;
				}
			}
		}
	}

	@Override
	public final void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			stop();
			callback.run();
		}
	}

	protected void stopInternal() throws Exception {
		if (isConnected()) {
			closeConnection();
		}
	}

	/**
	 * Return whether this ConnectionManager has been started.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Whether the connection is open/{@code true} or closed/{@code false}.
	 */
	public abstract boolean isConnected();

	/**
	 * Subclasses implement this to actually establish the connection.
	 */
	protected abstract void openConnection();

	/**
	 * Subclasses implement this to close the connection.
	 */
	protected abstract void closeConnection() throws Exception;

}
