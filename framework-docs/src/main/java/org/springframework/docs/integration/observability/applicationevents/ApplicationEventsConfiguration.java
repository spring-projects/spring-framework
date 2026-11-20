/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.applicationevents;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

@Configuration
public class ApplicationEventsConfiguration {

	@Bean(name = "applicationEventMulticaster")
	public SimpleApplicationEventMulticaster simpleApplicationEventMulticaster() {
		SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		// decorate task execution with a decorator that supports context propagation
		taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
		eventMulticaster.setTaskExecutor(taskExecutor);
		return eventMulticaster;
	}

}
