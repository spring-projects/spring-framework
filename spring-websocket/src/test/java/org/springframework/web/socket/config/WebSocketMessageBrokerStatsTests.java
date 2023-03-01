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

package org.springframework.web.socket.config;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WebSocketMessageBrokerStats}.
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

		assertThat(stats.getClientInboundExecutorStatsInfo()).as("inbound channel stats").isEqualTo("unknown");
		assertThat(stats.getClientOutboundExecutorStatsInfo()).as("outbound channel stats").isEqualTo("unknown");
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

		assertThat(stats.getSockJsTaskSchedulerStatsInfo()).isEqualTo("unknown");
	}

}
