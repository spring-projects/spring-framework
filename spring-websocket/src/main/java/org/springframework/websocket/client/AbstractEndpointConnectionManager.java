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

package org.springframework.websocket.client;

import java.io.IOException;
import java.net.URI;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.util.UriComponentsBuilder;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractEndpointConnectionManager implements SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final URI uri;

	private boolean autoStartup = false;

	private int phase = Integer.MAX_VALUE;

	private final WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();

	private Session session;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("EndpointConnectionManager-");

	private final Object lifecycleMonitor = new Object();


	public AbstractEndpointConnectionManager(String uriTemplate, Object... uriVariables) {
		this.uri = UriComponentsBuilder.fromUriString(uriTemplate).buildAndExpand(uriVariables).encode().toUri();
	}

	public void setAsyncSendTimeout(long timeoutInMillis) {
		this.webSocketContainer.setAsyncSendTimeout(timeoutInMillis);
	}

	public void setMaxSessionIdleTimeout(long timeoutInMillis) {
		this.webSocketContainer.setDefaultMaxSessionIdleTimeout(timeoutInMillis);
	}

	public void setMaxTextMessageBufferSize(int bufferSize) {
		this.webSocketContainer.setDefaultMaxTextMessageBufferSize(bufferSize);
	}

	public void setMaxBinaryMessageBufferSize(Integer bufferSize) {
		this.webSocketContainer.setDefaultMaxBinaryMessageBufferSize(bufferSize);
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
	public int getPhase() {
		return this.phase;
	}

	protected URI getUri() {
		return this.uri;
	}

	protected WebSocketContainer getWebSocketContainer() {
		return this.webSocketContainer;
	}

	/**
	 * Auto-connects to the configured {@link #setDefaultUri(URI) default URI}.
	 */
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				this.taskExecutor.execute(new Runnable() {
					@Override
					public void run() {
						synchronized (lifecycleMonitor) {
							try {
								logger.info("Connecting to endpoint at URI " + uri);
								session = connect();
								logger.info("Successfully connected");
							}
							catch (Throwable ex) {
								logger.error("Failed to connect to endpoint at " + uri, ex);
							}
						}
					}
				});
			}
		}
	}

	protected abstract Session connect() throws DeploymentException, IOException;

	/**
	 * Deactivates the configured message endpoint.
	 */
	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				try {
					this.session.close();
				}
				catch (IOException e) {
					// ignore
				}
			}
			this.session = null;
		}
	}

	public void stop(Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			this.stop();
			callback.run();
		}
	}

	/**
	 * Return whether the configured message endpoint is currently active.
	 */
	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			if ((this.session != null) && this.session.isOpen()) {
				return true;
			}
			this.session = null;
			return false;
		}
	}

}
