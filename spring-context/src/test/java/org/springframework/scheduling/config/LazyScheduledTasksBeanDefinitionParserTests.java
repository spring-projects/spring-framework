/*
 * Copyright 2002-present the original author or authors.
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
