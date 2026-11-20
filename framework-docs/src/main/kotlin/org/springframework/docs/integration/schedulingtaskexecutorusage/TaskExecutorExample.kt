/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.schedulingtaskexecutorusage

import org.springframework.core.task.TaskExecutor

// tag::snippet[]
class TaskExecutorExample(private val taskExecutor: TaskExecutor) {

	private inner class MessagePrinterTask(private val message: String) : Runnable {
		override fun run() {
			println(message)
		}
	}

	fun printMessages() {
		for (i in 0..24) {
			taskExecutor.execute(
				MessagePrinterTask(
					"Message$i"
				)
			)
		}
	}
}
// end::snippet[]