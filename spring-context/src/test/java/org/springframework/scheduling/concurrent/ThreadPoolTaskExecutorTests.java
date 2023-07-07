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

package org.springframework.scheduling.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncListenableTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Unit tests for {@link ThreadPoolTaskExecutor}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 5.0.5
 */
class ThreadPoolTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

	private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();


	@Override
	protected AsyncListenableTaskExecutor buildExecutor() {
		executor.setThreadNamePrefix(this.threadNamePrefix);
		executor.setMaxPoolSize(1);
		executor.afterPropertiesSet();
		return executor;
	}


	@Test
	void modifyCorePoolSizeWhileRunning() {
		assertThat(executor.getCorePoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);

		executor.setCorePoolSize(0);

		assertThat(executor.getCorePoolSize()).isZero();
		assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isZero();
	}

	@Test
	void modifyCorePoolSizeWithInvalidValueWhileRunning() {
		assertThat(executor.getCorePoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);

		assertThatIllegalArgumentException().isThrownBy(() -> executor.setCorePoolSize(-1));

		assertThat(executor.getCorePoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
	}

	@Test
	void modifyMaxPoolSizeWhileRunning() {
		assertThat(executor.getMaxPoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);

		executor.setMaxPoolSize(5);

		assertThat(executor.getMaxPoolSize()).isEqualTo(5);
		assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(5);
	}

	@Test
	void modifyMaxPoolSizeWithInvalidValueWhileRunning() {
		assertThat(executor.getMaxPoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);

		assertThatIllegalArgumentException().isThrownBy(() -> executor.setMaxPoolSize(0));

		assertThat(executor.getMaxPoolSize()).isEqualTo(1);
		assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);
	}

	@Test
	void modifyKeepAliveSecondsWhileRunning() {
		assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
		assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);

		executor.setKeepAliveSeconds(10);

		assertThat(executor.getKeepAliveSeconds()).isEqualTo(10);
		assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(10);
	}

	@Test
	void modifyKeepAliveSecondsWithInvalidValueWhileRunning() {
		assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
		assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);

		assertThatIllegalArgumentException().isThrownBy(() -> executor.setKeepAliveSeconds(-10));

		assertThat(executor.getKeepAliveSeconds()).isEqualTo(60);
		assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);
	}

	@Test
	void queueCapacityDefault() {
		assertThat(executor.getQueueCapacity()).isEqualTo(Integer.MAX_VALUE);
		assertThat(executor.getThreadPoolExecutor().getQueue())
				.asInstanceOf(type(LinkedBlockingQueue.class))
				.extracting(BlockingQueue::remainingCapacity).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void queueCapacityZero() {
		executor.setQueueCapacity(0);
		executor.afterPropertiesSet();

		assertThat(executor.getQueueCapacity()).isZero();
		assertThat(executor.getThreadPoolExecutor().getQueue())
				.asInstanceOf(type(SynchronousQueue.class))
				.extracting(BlockingQueue::remainingCapacity).isEqualTo(0);
	}

	@Test
	void queueSize() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		assertThatIllegalStateException().isThrownBy(executor::getThreadPoolExecutor);
		assertThat(executor.getQueueSize()).isZero();

		executor.afterPropertiesSet();

		assertThat(executor.getThreadPoolExecutor()).isNotNull();
		assertThat(executor.getThreadPoolExecutor().getQueue()).isEmpty();
		assertThat(executor.getQueueSize()).isZero();
	}

}
