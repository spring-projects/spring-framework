/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.concurrent;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.support.DelegatingErrorHandlingRunnable;
import org.springframework.scheduling.support.TaskUtils;

/**
 * @author Juergen Hoeller
 * @since 5.0.5
 */
class DecoratedThreadPoolTaskExecutorTests extends AbstractSchedulingTaskExecutorTests {

	@Override
	protected AsyncTaskExecutor buildExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setTaskDecorator(runnable ->
				new DelegatingErrorHandlingRunnable(runnable, TaskUtils.LOG_AND_PROPAGATE_ERROR_HANDLER));
		executor.setThreadNamePrefix(this.threadNamePrefix);
		executor.setMaxPoolSize(1);
		executor.afterPropertiesSet();
		return executor;
	}

}
