/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingtaskexecutorusage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.TaskDecorator;

public class LoggingTaskDecorator implements TaskDecorator {

	private static final Log logger = LogFactory.getLog(LoggingTaskDecorator.class);

	@Override
	public Runnable decorate(Runnable runnable) {
		return () -> {
			logger.debug("Before execution of " + runnable);
			runnable.run();
			logger.debug("After execution of " + runnable);
		};
	}
}
