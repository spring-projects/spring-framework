/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingtaskexecutorusage

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.task.TaskDecorator

class LoggingTaskDecorator : TaskDecorator {

	override fun decorate(runnable: Runnable): Runnable {
		return Runnable {
			logger.debug("Before execution of $runnable")
			runnable.run()
			logger.debug("After execution of $runnable")
		}
	}

	companion object {
		private val logger: Log = LogFactory.getLog(
			LoggingTaskDecorator::class.java
		)
	}
}
