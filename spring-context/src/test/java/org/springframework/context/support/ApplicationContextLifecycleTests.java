/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.context.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public class ApplicationContextLifecycleTests {

	@Test
	public void testBeansStart() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String error = "bean was not started";
		assertTrue(error, bean1.isRunning());
		assertTrue(error, bean2.isRunning());
		assertTrue(error, bean3.isRunning());
		assertTrue(error, bean4.isRunning());
	}

	@Test
	public void testBeansStop() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String startError = "bean was not started";
		assertTrue(startError, bean1.isRunning());
		assertTrue(startError, bean2.isRunning());
		assertTrue(startError, bean3.isRunning());
		assertTrue(startError, bean4.isRunning());
		context.stop();
		String stopError = "bean was not stopped";
		assertFalse(stopError, bean1.isRunning());
		assertFalse(stopError, bean2.isRunning());
		assertFalse(stopError, bean3.isRunning());
		assertFalse(stopError, bean4.isRunning());
	}

	@Test
	public void testStartOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStartedError = "bean was not started";
		assertTrue(notStartedError, bean1.getStartOrder() > 0);
		assertTrue(notStartedError, bean2.getStartOrder() > 0);
		assertTrue(notStartedError, bean3.getStartOrder() > 0);
		assertTrue(notStartedError, bean4.getStartOrder() > 0);
		String orderError = "dependent bean must start after the bean it depends on";
		assertTrue(orderError, bean2.getStartOrder() > bean1.getStartOrder());
		assertTrue(orderError, bean3.getStartOrder() > bean2.getStartOrder());
		assertTrue(orderError, bean4.getStartOrder() > bean2.getStartOrder());
	}

	@Test
	public void testStopOrder() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		context.stop();
		LifecycleTestBean bean1 = (LifecycleTestBean) context.getBean("bean1");
		LifecycleTestBean bean2 = (LifecycleTestBean) context.getBean("bean2");
		LifecycleTestBean bean3 = (LifecycleTestBean) context.getBean("bean3");
		LifecycleTestBean bean4 = (LifecycleTestBean) context.getBean("bean4");
		String notStoppedError = "bean was not stopped";
		assertTrue(notStoppedError, bean1.getStopOrder() > 0);
		assertTrue(notStoppedError, bean2.getStopOrder() > 0);
		assertTrue(notStoppedError, bean3.getStopOrder() > 0);
		assertTrue(notStoppedError, bean4.getStopOrder() > 0);
		String orderError = "dependent bean must stop before the bean it depends on";
		assertTrue(orderError, bean2.getStopOrder() < bean1.getStopOrder());
		assertTrue(orderError, bean3.getStopOrder() < bean2.getStopOrder());
		assertTrue(orderError, bean4.getStopOrder() < bean2.getStopOrder());
	}

}
