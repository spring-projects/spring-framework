/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.config;

import java.util.concurrent.Executor;

import org.jspecify.annotations.Nullable;

import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.backoff.BackOff;

/**
 * A {@link JmsListenerContainerFactory} implementation to build a regular
 * {@link DefaultMessageListenerContainer}.
 *
 * <p>This should be the default for most users and a good transition path
 * for those who are used to building such a container definition manually.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class DefaultJmsListenerContainerFactory
		extends AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> {

	private @Nullable Executor taskExecutor;

	private @Nullable PlatformTransactionManager transactionManager;

	private @Nullable Integer cacheLevel;

	private @Nullable String cacheLevelName;

	private @Nullable String concurrency;

	private @Nullable Integer maxMessagesPerTask;

	private @Nullable Long receiveTimeout;

	private @Nullable Long recoveryInterval;

	private @Nullable BackOff backOff;


	/**
	 * @see DefaultMessageListenerContainer#setTaskExecutor
	 */
	public void setTaskExecutor(@Nullable Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @see DefaultMessageListenerContainer#setTransactionManager
	 */
	public void setTransactionManager(@Nullable PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * @see DefaultMessageListenerContainer#setCacheLevel
	 */
	public void setCacheLevel(@Nullable Integer cacheLevel) {
		this.cacheLevel = cacheLevel;
	}

	/**
	 * @see DefaultMessageListenerContainer#setCacheLevelName
	 */
	public void setCacheLevelName(@Nullable String cacheLevelName) {
		this.cacheLevelName = cacheLevelName;
	}

	/**
	 * @see DefaultMessageListenerContainer#setConcurrency
	 */
	public void setConcurrency(@Nullable String concurrency) {
		this.concurrency = concurrency;
	}

	/**
	 * @see DefaultMessageListenerContainer#setMaxMessagesPerTask
	 */
	public void setMaxMessagesPerTask(@Nullable Integer maxMessagesPerTask) {
		this.maxMessagesPerTask = maxMessagesPerTask;
	}

	/**
	 * @see DefaultMessageListenerContainer#setReceiveTimeout
	 */
	public void setReceiveTimeout(@Nullable Long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * @see DefaultMessageListenerContainer#setRecoveryInterval
	 */
	public void setRecoveryInterval(@Nullable Long recoveryInterval) {
		this.recoveryInterval = recoveryInterval;
	}

	/**
	 * @see DefaultMessageListenerContainer#setBackOff
	 */
	public void setBackOff(@Nullable BackOff backOff) {
		this.backOff = backOff;
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

		if (this.backOff != null) {
			container.setBackOff(this.backOff);
			if (this.recoveryInterval != null) {
				logger.info("Ignoring recovery interval in DefaultJmsListenerContainerFactory in favor of BackOff");
			}
		}
		else if (this.recoveryInterval != null) {
			container.setRecoveryInterval(this.recoveryInterval);
		}
	}

}
