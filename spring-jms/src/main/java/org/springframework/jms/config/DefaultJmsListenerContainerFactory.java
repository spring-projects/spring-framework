/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.jms.config;

import java.util.concurrent.Executor;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A {@link JmsListenerContainerFactory} implementation to build regular
 * {@link DefaultMessageListenerContainer}.
 *
 * <p>This should be the default for most users and a good transition
 * paths for those that are used to build such container definition
 * manually.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class DefaultJmsListenerContainerFactory
		extends AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> {

	private Executor taskExecutor;

	private PlatformTransactionManager transactionManager;

	private Integer cacheLevel;

	private String cacheLevelName;

	private String concurrency;

	private Integer maxMessagesPerTask;

	private Long receiveTimeout;

	private Long recoveryInterval;

	/**
	 * @see DefaultMessageListenerContainer#setTaskExecutor(java.util.concurrent.Executor)
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @see DefaultMessageListenerContainer#setTransactionManager(PlatformTransactionManager)
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * @see DefaultMessageListenerContainer#setCacheLevel(int)
	 */
	public void setCacheLevel(Integer cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	/**
	 * @see DefaultMessageListenerContainer#setCacheLevelName(String)
	 */
	public void setCacheLevelName(String cacheLevelName) {
		this.cacheLevelName = cacheLevelName;
	}

	/**
	 * @see DefaultMessageListenerContainer#setConcurrency(String)
	 */
	public void setConcurrency(String concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * @see DefaultMessageListenerContainer#setMaxMessagesPerTask(int)
	 */
	public void setMaxMessagesPerTask(Integer maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	/**
	 * @see DefaultMessageListenerContainer#setReceiveTimeout(long)
	 */
	public void setReceiveTimeout(Long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * @see DefaultMessageListenerContainer#setRecoveryInterval(long)
	 */
	public void setRecoveryInterval(Long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	@Override
	protected DefaultMessageListenerContainer createContainerInstance() {
		return new DefaultMessageListenerContainer();
	}

	@Override
	protected void initializeContainer(DefaultMessageListenerContainer container) {
		if (this.taskExecutor != null) {
			container.setTaskExecutor(this.taskExecutor);
		}
		if (this.transactionManager != null) {
			container.setTransactionManager(this.transactionManager);
		}

		if (this.cacheLevel != null) {
			container.setCacheLevel(this.cacheLevel);
		}
		else if (this.cacheLevelName != null) {
			container.setCacheLevelName(this.cacheLevelName);
		}

		if (this.concurrency != null) {
			container.setConcurrency(this.concurrency);
		}
		if (this.maxMessagesPerTask != null) {
			container.setMaxMessagesPerTask(this.maxMessagesPerTask);
		}
		if (this.receiveTimeout != null) {
			container.setReceiveTimeout(this.receiveTimeout);
		}
		if (this.recoveryInterval != null) {
			container.setRecoveryInterval(this.recoveryInterval);
		}
	}

}
