/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.scheduling.quartz;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StopWatch;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class QuartzSchedulerLifecycleTests {

	@Test // SPR-6354
	public void destroyLazyInitSchedulerWithDefaultShutdownOrderDoesNotHang() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("quartzSchedulerLifecycleTests.xml", this.getClass());
		assertNotNull(context.getBean("lazyInitSchedulerWithDefaultShutdownOrder"));
		StopWatch sw = new StopWatch();
		sw.start("lazyScheduler");
		context.destroy();
		sw.stop();
		assertTrue("Quartz Scheduler with lazy-init is hanging on destruction: " +
				sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 500);
	}

	@Test // SPR-6354
	public void destroyLazyInitSchedulerWithCustomShutdownOrderDoesNotHang() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("quartzSchedulerLifecycleTests.xml", this.getClass());
		assertNotNull(context.getBean("lazyInitSchedulerWithCustomShutdownOrder"));
		StopWatch sw = new StopWatch();
		sw.start("lazyScheduler");
		context.destroy();
		sw.stop();
		assertTrue("Quartz Scheduler with lazy-init is hanging on destruction: " +
				sw.getTotalTimeMillis(), sw.getTotalTimeMillis() < 500);
	}

}
