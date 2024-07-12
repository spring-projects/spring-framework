/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.socket.config;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.task.TaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * A central class for aggregating information about internal state and counters
 * from key infrastructure components of the setup that comes with
 * {@code @EnableWebSocketMessageBroker} for Java config and
 * {@code <websocket:message-broker>} for XML.
 *
 * <p>By default aggregated information is logged every 30 minutes at INFO level.
 * The frequency of logging can be changed via {@link #setLoggingPeriod(long)}.
 *
 * <p>This class is declared as a Spring bean by the above configuration with the
 * name "webSocketMessageBrokerStats" and can be easily exported to JMX, e.g. with
 * the {@link org.springframework.jmx.export.MBeanExporter MBeanExporter}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 4.1
 */
public class WebSocketMessageBrokerStats implements SmartInitializingSingleton {

	private static final Log logger = LogFactory.getLog(WebSocketMessageBrokerStats.class);


	@Nullable
	private SubProtocolWebSocketHandler webSocketHandler;

	@Nullable
	private StompSubProtocolHandler stompSubProtocolHandler;

	@Nullable
	private StompBrokerRelayMessageHandler stompBrokerRelay;

	@Nullable
	private TaskExecutor inboundChannelExecutor;

	@Nullable
	private TaskExecutor outboundChannelExecutor;

	@Nullable
	private TaskScheduler sockJsTaskScheduler;

	@Nullable
	private ScheduledFuture<?> loggingTask;

	private long loggingPeriod = TimeUnit.MINUTES.toMillis(30);


	public void setSubProtocolWebSocketHandler(SubProtocolWebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
	}

	public void setStompBrokerRelay(StompBrokerRelayMessageHandler stompBrokerRelay) {
		this.stompBrokerRelay = stompBrokerRelay;
	}

	public void setInboundChannelExecutor(TaskExecutor inboundChannelExecutor) {
		this.inboundChannelExecutor = inboundChannelExecutor;
	}

	public void setOutboundChannelExecutor(TaskExecutor outboundChannelExecutor) {
		this.outboundChannelExecutor = outboundChannelExecutor;
	}

	public void setSockJsTaskScheduler(TaskScheduler sockJsTaskScheduler) {
		this.sockJsTaskScheduler = sockJsTaskScheduler;
	}

	/**
	 * Set the frequency for logging information at INFO level in milliseconds.
	 * If set 0 or less than 0, the logging task is cancelled.
	 * <p>By default this property is set to 30 minutes (30 * 60 * 1000).
	 */
	public void setLoggingPeriod(long period) {
		this.loggingPeriod = period;
		if (this.loggingTask != null) {
			this.loggingTask.cancel(true);
			this.loggingTask = initLoggingTask(0);
		}
	}

	/**
	 * Return the configured logging period frequency in milliseconds.
	 */
	public long getLoggingPeriod() {
		return this.loggingPeriod;
	}


	@Override
	public void afterSingletonsInstantiated() {
		this.stompSubProtocolHandler = initStompSubProtocolHandler();
		this.loggingTask = initLoggingTask(TimeUnit.MINUTES.toMillis(1));
	}

	@Nullable
	private StompSubProtocolHandler initStompSubProtocolHandler() {
		if (this.webSocketHandler == null) {
			return null;
		}
		for (SubProtocolHandler handler : this.webSocketHandler.getProtocolHandlers()) {
			if (handler instanceof StompSubProtocolHandler stompHandler) {
				return stompHandler;
			}
		}
		SubProtocolHandler defaultHandler = this.webSocketHandler.getDefaultProtocolHandler();
		if (defaultHandler instanceof StompSubProtocolHandler stompHandler) {
			return stompHandler;
		}
		return null;
	}

	@Nullable
	private ScheduledFuture<?> initLoggingTask(long initialDelay) {
		if (this.sockJsTaskScheduler != null && this.loggingPeriod > 0 && logger.isInfoEnabled()) {
			return this.sockJsTaskScheduler.scheduleWithFixedDelay(
					() -> logger.info(WebSocketMessageBrokerStats.this.toString()),
					Instant.now().plusMillis(initialDelay), Duration.ofMillis(this.loggingPeriod));
		}
		return null;
	}


	/**
	 * Get stats about WebSocket sessions.
	 * @deprecated as of 6.2 in favor of {@link #getWebSocketSessionStats()}.
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public String getWebSocketSessionStatsInfo() {
		return (this.webSocketHandler != null ? this.webSocketHandler.getStatsInfo() : "null");
	}

	/**
	 * Get stats about WebSocket sessions.
	 * Can return {@code null} if no {@link #setSubProtocolWebSocketHandler(SubProtocolWebSocketHandler) WebSocket handler}
	 * is configured.
	 * @since 6.2
	 */
	@Nullable
	public SubProtocolWebSocketHandler.Stats getWebSocketSessionStats() {
		return (this.webSocketHandler != null ? this.webSocketHandler.getStats() : null);
	}

	/**
	 * Get stats about STOMP-related WebSocket message processing.
	 * @deprecated as of 6.2 in favor of {@link #getStompSubProtocolStats()}.
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public String getStompSubProtocolStatsInfo() {
		return (this.stompSubProtocolHandler != null ? this.stompSubProtocolHandler.getStatsInfo() : "null");
	}

	/**
	 * Get stats about STOMP-related WebSocket message processing.
	 * Can return {@code null} if no {@link SubProtocolHandler} was found.
	 * @since 6.2
	 */
	@Nullable
	public StompSubProtocolHandler.Stats getStompSubProtocolStats() {
		return (this.stompSubProtocolHandler != null ? this.stompSubProtocolHandler.getStats() : null);
	}

	/**
	 * Get stats about STOMP broker relay (when using a full-featured STOMP broker).
	 * @deprecated as of 6.2 in favor of {@link #getStompBrokerRelayStats()}.
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public String getStompBrokerRelayStatsInfo() {
		return (this.stompBrokerRelay != null ? this.stompBrokerRelay.getStatsInfo() : "null");
	}

	/**
	 * Get stats about STOMP broker relay (when using a full-featured STOMP broker).
	 * Can return {@code null} if no {@link #setStompBrokerRelay(StompBrokerRelayMessageHandler) STOMP broker relay}
	 * is configured.
	 * @since 6.2
	 */
	@Nullable
	public StompBrokerRelayMessageHandler.Stats getStompBrokerRelayStats() {
		return (this.stompBrokerRelay != null ? this.stompBrokerRelay.getStats() : null);
	}

	/**
	 * Get stats about the executor processing incoming messages from WebSocket clients.
	 */
	public String getClientInboundExecutorStatsInfo() {
		return getExecutorStatsInfo(this.inboundChannelExecutor);
	}

	/**
	 * Get stats about the executor processing outgoing messages to WebSocket clients.
	 */
	public String getClientOutboundExecutorStatsInfo() {
		return getExecutorStatsInfo(this.outboundChannelExecutor);
	}

	/**
	 * Get stats about the SockJS task scheduler.
	 */
	public String getSockJsTaskSchedulerStatsInfo() {
		if (this.sockJsTaskScheduler == null) {
			return "null";
		}

		if (!(this.sockJsTaskScheduler instanceof SchedulingTaskExecutor)) {
			return "thread-per-task";
		}

		if (this.sockJsTaskScheduler instanceof ThreadPoolTaskScheduler tpts) {
			return getExecutorStatsInfo(tpts.getScheduledThreadPoolExecutor());
		}

		return "unknown";
	}

	private String getExecutorStatsInfo(@Nullable Executor executor) {
		if (executor == null) {
			return "null";
		}

		if (!(executor instanceof SchedulingTaskExecutor) && (executor instanceof TaskExecutor)) {
			return "thread-per-task";
		}

		if (executor instanceof ThreadPoolTaskExecutor tpte) {
			executor = tpte.getThreadPoolExecutor();
		}

		if (executor instanceof ThreadPoolExecutor) {
			// It is assumed that the implementation of toString() in ThreadPoolExecutor
			// generates text that ends similar to the following:
			// pool size = #, active threads = #, queued tasks = #, completed tasks = #]
			String str = executor.toString();
			int indexOfPool = str.indexOf("pool");
			if (indexOfPool != -1) {
				// (length - 1) omits the trailing "]"
				return str.substring(indexOfPool, str.length() - 1);
			}
		}

		return "unknown";
	}

	@Override
	@SuppressWarnings("removal")
	public String toString() {
		return "WebSocketSession[" + getWebSocketSessionStatsInfo() + "]" +
				", stompSubProtocol[" + getStompSubProtocolStatsInfo() + "]" +
				", stompBrokerRelay[" + getStompBrokerRelayStatsInfo() + "]" +
				", inboundChannel[" + getClientInboundExecutorStatsInfo() + "]" +
				", outboundChannel[" + getClientOutboundExecutorStatsInfo() + "]" +
				", sockJsScheduler[" + getSockJsTaskSchedulerStatsInfo() + "]";
	}

}
