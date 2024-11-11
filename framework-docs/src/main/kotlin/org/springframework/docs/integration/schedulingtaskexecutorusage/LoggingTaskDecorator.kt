/*
 * Copyright 2002-2024 the original author or authors.
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
