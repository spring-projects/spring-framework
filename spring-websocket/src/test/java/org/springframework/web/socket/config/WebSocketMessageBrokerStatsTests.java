/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebSocketMessageBrokerStats}.
 *
 * @author Sam Brannen
 * @since 5.3.10
 * @see org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupportTests
 */
class WebSocketMessageBrokerStatsTests {

	private final WebSocketMessageBrokerStats stats = new WebSocketMessageBrokerStats();

	@Test
	void nullValues() {
		String expected = "WebSocketSession[null], stompSubProtocol[null], stompBrokerRelay[null], " +
				"inboundChannel[null], outboundChannel[null], sockJsScheduler[null]";
		assertThat(stats).hasToString(expected);
	}

	@Test
	void inboundAndOutboundChannelsWithThreadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.afterPropertiesSet();

		stats.setInboundChannelExecutor(executor);
		stats.setOutboundChannelExecutor(executor);

		assertThat(stats.getClientInboundExecutorStatsInfo()).as("inbound channel stats")
			.isEqualTo("pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0");
		assertThat(stats.getClientOutboundExecutorStatsInfo()).as("outbound channel stats")
			.isEqualTo("pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0");
	}

	@Test
	void inboundAndOutboundChannelsWithMockedTaskExecutor() {
		TaskExecutor executor = mock();

		stats.setInboundChannelExecutor(executor);
		stats.setOutboundChannelExecutor(executor);

		assertThat(stats.getClientInboundExecutorStatsInfo()).as("inbound channel stats").isEqualTo("thread-per-task");
		assertThat(stats.getClientOutboundExecutorStatsInfo()).as("outbound channel stats").isEqualTo("thread-per-task");
	}

	@Test
	void sockJsTaskSchedulerWithThreadPoolTaskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();

		stats.setSockJsTaskScheduler(scheduler);

		assertThat(stats.getSockJsTaskSchedulerStatsInfo())
			.isEqualTo("pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0");
	}

	@Test
	void sockJsTaskSchedulerWithMockedTaskScheduler() {
		TaskScheduler scheduler = mock();

		stats.setSockJsTaskScheduler(scheduler);

		assertThat(stats.getSockJsTaskSchedulerStatsInfo()).isEqualTo("thread-per-task");
	}

}
