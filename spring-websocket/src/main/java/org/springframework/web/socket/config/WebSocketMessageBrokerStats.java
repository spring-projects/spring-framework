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

package org.springframework.web.socket.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
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
 * <p>By default aggregated information is logged every 15 minutes at INFO level.
 * The frequency of logging can be changed via {@link #setLoggingPeriod(long)}.
 *
 * <p>This class is declared as a Spring bean by the above configuration with the
 * name "webSocketMessageBrokerStats" and can be easily exported to JMX, e.g. with
 * the {@link org.springframework.jmx.export.MBeanExporter MBeanExporter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class WebSocketMessageBrokerStats {

	private static final Log logger = LogFactory.getLog(WebSocketMessageBrokerStats.class);


	@Nullable
	private SubProtocolWebSocketHandler webSocketHandler;

	@Nullable
	private StompSubProtocolHandler stompSubProtocolHandler;

	@Nullable
	private StompBrokerRelayMessageHandler stompBrokerRelay;

	@Nullable
	private ThreadPoolExecutor inboundChannelExecutor;

	@Nullable
	private ThreadPoolExecutor outboundChannelExecutor;

	@Nullable
	private ScheduledThreadPoolExecutor sockJsTaskScheduler;

	@Nullable
	private ScheduledFuture<?> loggingTask;

	private long loggingPeriod = 30 * 60 * 1000;


	public void setSubProtocolWebSocketHandler(SubProtocolWebSocketHandler webSocketHandler) {
		this.webSocketHandler = webSocketHandler;
		this.stompSubProtocolHandler = initStompSubProtocolHandler();
	}

	@Nullable
	private StompSubProtocolHandler initStompSubProtocolHandler() {
		if (this.webSocketHandler == null) {
			return null;
		}
		for (SubProtocolHandler handler : this.webSocketHandler.getProtocolHandlers()) {
			if (handler instanceof StompSubProtocolHandler) {
				return (StompSubProtocolHandler) handler;
			}
		}
		SubProtocolHandler defaultHandler = this.webSocketHandler.getDefaultProtocolHandler();
		if (defaultHandler != null && defaultHandler instanceof StompSubProtocolHandler) {
			return (StompSubProtocolHandler) defaultHandler;
		}
		return null;
	}

	public void setStompBrokerRelay(StompBrokerRelayMessageHandler stompBrokerRelay) {
		this.stompBrokerRelay = stompBrokerRelay;
	}

	public void setInboundChannelExecutor(ThreadPoolTaskExecutor inboundChannelExecutor) {
		this.inboundChannelExecutor = inboundChannelExecutor.getThreadPoolExecutor();
	}

	public void setOutboundChannelExecutor(ThreadPoolTaskExecutor outboundChannelExecutor) {
		this.outboundChannelExecutor = outboundChannelExecutor.getThreadPoolExecutor();
	}

	public void setSockJsTaskScheduler(ThreadPoolTaskScheduler sockJsTaskScheduler) {
		this.sockJsTaskScheduler = sockJsTaskScheduler.getScheduledThreadPoolExecutor();
		this.loggingTask = initLoggingTask(60 * 1000);
	}

	@Nullable
	private ScheduledFuture<?> initLoggingTask(long initialDelay) {
		if (this.sockJsTaskScheduler != null && this.loggingPeriod > 0 && logger.isInfoEnabled()) {
			return this.sockJsTaskScheduler.scheduleAtFixedRate(() ->
							logger.info(WebSocketMessageBrokerStats.this.toString()),
					initialDelay, this.loggingPeriod, TimeUnit.MILLISECONDS);
		}
		return null;
	}

	/**
	 * Set the frequency for logging information at INFO level in milliseconds.
	 * If set 0 or less than 0, the logging task is cancelled.
	 * <p>By default this property is set to 30 minutes (30 * 60 * 1000).
	 */
	public void setLoggingPeriod(long period) {
		if (this.loggingTask != null) {
			this.loggingTask.cancel(true);
		}
		this.loggingPeriod = period;
		this.loggingTask = initLoggingTask(0);
	}

	/**
	 * Return the configured logging period frequency in milliseconds.
	 */
	public long getLoggingPeriod() {
		return this.loggingPeriod;
	}

	/**
	 * Get stats about WebSocket sessions.
	 */
	public String getWebSocketSessionStatsInfo() {
		return (this.webSocketHandler != null ? this.webSocketHandler.getStatsInfo() : "null");
	}

	/**
	 * Get stats about STOMP-related WebSocket message processing.
	 */
	public String getStompSubProtocolStatsInfo() {
		return (this.stompSubProtocolHandler != null ? this.stompSubProtocolHandler.getStatsInfo() : "null");
	}

	/**
	 * Get stats about STOMP broker relay (when using a full-featured STOMP broker).
	 */
	public String getStompBrokerRelayStatsInfo() {
		return (this.stompBrokerRelay != null ? this.stompBrokerRelay.getStatsInfo() : "null");
	}

	/**
	 * Get stats about the executor processing incoming messages from WebSocket clients.
	 */
	public String getClientInboundExecutorStatsInfo() {
		return (this.inboundChannelExecutor != null ? getExecutorStatsInfo(this.inboundChannelExecutor) : "null");
	}

	/**
	 * Get stats about the executor processing outgoing messages to WebSocket clients.
	 */
	public String getClientOutboundExecutorStatsInfo() {
		return (this.outboundChannelExecutor != null ? getExecutorStatsInfo(this.outboundChannelExecutor) : "null");
	}

	/**
	 * Get stats about the SockJS task scheduler.
	 */
	public String getSockJsTaskSchedulerStatsInfo() {
		return (this.sockJsTaskScheduler != null ? getExecutorStatsInfo(this.sockJsTaskScheduler) : "null");
	}

	private String getExecutorStatsInfo(Executor executor) {
		String str = executor.toString();
		return str.substring(str.indexOf("pool"), str.length() - 1);
	}

	public String toString() {
		return "WebSocketSession[" + getWebSocketSessionStatsInfo() + "]" +
				", stompSubProtocol[" + getStompSubProtocolStatsInfo() + "]" +
				", stompBrokerRelay[" + getStompBrokerRelayStatsInfo() + "]" +
				", inboundChannel[" + getClientInboundExecutorStatsInfo() + "]" +
				", outboundChannel" + getClientOutboundExecutorStatsInfo() + "]" +
				", sockJsScheduler[" + getSockJsTaskSchedulerStatsInfo() + "]";
	}

}
