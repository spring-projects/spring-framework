/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.scheduling.quartz;

import org.junit.jupiter.api.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class QuartzSchedulerLifecycleTests {

	@Test  // SPR-6354
	public void destroyLazyInitSchedulerWithDefaultShutdownOrderDoesNotHang() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("quartzSchedulerLifecycleTests.xml", getClass());
		assertThat(context.getBean("lazyInitSchedulerWithDefaultShutdownOrder")).isNotNull();
		StopWatch sw = new StopWatch();
		sw.start("lazyScheduler");
		context.close();
		sw.stop();
		assertThat(sw.getTotalTimeMillis()).as("Quartz Scheduler with lazy-init is hanging on destruction: " +
				sw.getTotalTimeMillis()).isLessThan(500);
	}

	@Test  // SPR-6354
	public void destroyLazyInitSchedulerWithCustomShutdownOrderDoesNotHang() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("quartzSchedulerLifecycleTests.xml", getClass());
		assertThat(context.getBean("lazyInitSchedulerWithCustomShutdownOrder")).isNotNull();
		StopWatch sw = new StopWatch();
		sw.start("lazyScheduler");
		context.close();
		sw.stop();
		assertThat(sw.getTotalTimeMillis()).as("Quartz Scheduler with lazy-init is hanging on destruction: " +
				sw.getTotalTimeMillis()).isLessThan(500);
	}

}
