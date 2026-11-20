/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingtaskexecutorusage

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class TaskExecutorConfiguration {

	// tag::snippet[]
	@Bean
	fun taskExecutor() = ThreadPoolTaskExecutor().apply {
		corePoolSize = 5
		maxPoolSize = 10
		queueCapacity = 25
	}

	@Bean
	fun taskExecutorExample(taskExecutor: ThreadPoolTaskExecutor) = TaskExecutorExample(taskExecutor)
	// end::snippet[]

	// tag::decorator[]
	@Bean
	fun decoratedTaskExecutor() = ThreadPoolTaskExecutor().apply {
		setTaskDecorator(LoggingTaskDecorator())
	}
	// end::decorator[]
}
