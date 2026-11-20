/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingtaskexecutorusage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfiguration {

	// tag::snippet[]
	@Bean
	ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(5);
		taskExecutor.setMaxPoolSize(10);
		taskExecutor.setQueueCapacity(25);
		return taskExecutor;
	}

	@Bean
	TaskExecutorExample taskExecutorExample(ThreadPoolTaskExecutor taskExecutor) {
		return new TaskExecutorExample(taskExecutor);
	}
	// end::snippet[]

	// tag::decorator[]
	@Bean
	ThreadPoolTaskExecutor decoratedTaskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setTaskDecorator(new LoggingTaskDecorator());
		return taskExecutor;
	}
	// end::decorator[]
}
