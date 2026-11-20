/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Tests ensuring that tasks scheduled using the &lt;task:scheduled&gt; element
 * are never marked lazy, even if the enclosing &lt;beans&gt; element declares
 * default-lazy-init="true". See  SPR-8498
 *
 * @author Mike Youngstrom
 * @author Chris Beams
 * @author Sam Brannen
 */
class LazyScheduledTasksBeanDefinitionParserTests {

	@Test
	@Timeout(5)
	void checkTarget() {
		try (ConfigurableApplicationContext applicationContext =
				new GenericXmlApplicationContext(getClass(), "lazyScheduledTasksContext.xml")) {

			Task task = applicationContext.getBean(Task.class);

			while (!task.executed) {
				try {
					Thread.sleep(10);
				}
				catch (Exception ex) {
					/* Do Nothing */
				}
			}
		}
	}


	static class Task {

		volatile boolean executed = false;

		public void doWork() {
			executed = true;
		}
	}

}
