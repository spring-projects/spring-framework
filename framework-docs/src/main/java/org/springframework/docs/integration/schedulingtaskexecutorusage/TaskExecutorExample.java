/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */


package org.springframework.docs.integration.schedulingtaskexecutorusage;

import org.springframework.core.task.TaskExecutor;

// tag::snippet[]
public class TaskExecutorExample {

	private class MessagePrinterTask implements Runnable {

		private String message;

		public MessagePrinterTask(String message) {
			this.message = message;
		}

		public void run() {
			System.out.println(message);
		}
	}

	private TaskExecutor taskExecutor;

	public TaskExecutorExample(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void printMessages() {
		for(int i = 0; i < 25; i++) {
			taskExecutor.execute(new MessagePrinterTask("Message" + i));
		}
	}
}
// end::snippet[]
