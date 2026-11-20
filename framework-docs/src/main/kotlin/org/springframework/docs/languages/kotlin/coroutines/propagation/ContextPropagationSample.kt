/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.languages.kotlin.coroutines.propagation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.core.PropagationContextElement

class ContextPropagationSample {

	companion object {
		private val logger: Log = LogFactory.getLog(
			ContextPropagationSample::class.java
		)
	}

	// tag::context[]
	fun main() {
		runBlocking(Dispatchers.IO + PropagationContextElement()) {
			waitAndLog()
		}
	}

	suspend fun waitAndLog() {
		delay(10)
		logger.info("Suspending function with traceId")
	}
	// end::context[]
}