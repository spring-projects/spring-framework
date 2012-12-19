/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.scheduling.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.TestCase;

import org.springframework.scheduling.TestMethodInvokingTask;

/**
 * @author Juergen Hoeller
 * @since 20.02.2004
 */
public class TimerSupportTests extends TestCase {

	public void testTimerFactoryBean() throws Exception {
		final TestTimerTask timerTask0 = new TestTimerTask();

		TestMethodInvokingTask task1 = new TestMethodInvokingTask();
		MethodInvokingTimerTaskFactoryBean mittfb = new MethodInvokingTimerTaskFactoryBean();
		mittfb.setTargetObject(task1);
		mittfb.setTargetMethod("doSomething");
		mittfb.afterPropertiesSet();
		final TimerTask timerTask1 = mittfb.getObject();

		final TestRunnable timerTask2 = new TestRunnable();

		ScheduledTimerTask[] tasks = new ScheduledTimerTask[3];
		tasks[0] = new ScheduledTimerTask(timerTask0, 0, 10, false);
		tasks[1] = new ScheduledTimerTask(timerTask1, 10, 20, true);
		tasks[2] = new ScheduledTimerTask(timerTask2, 20);

		final List<Boolean> success = new ArrayList<Boolean>(3);
		final Timer timer = new Timer(true) {
			public void schedule(TimerTask task, long delay, long period) {
				if (task == timerTask0 && delay == 0 && period == 10) {
					success.add(Boolean.TRUE);
				}
			}
			public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
				if (task == timerTask1 && delay == 10 && period == 20) {
					success.add(Boolean.TRUE);
				}
			}
			public void schedule(TimerTask task, long delay) {
				if (task instanceof DelegatingTimerTask && delay == 20) {
					success.add(Boolean.TRUE);
				}
			}
			public void cancel() {
				success.add(Boolean.TRUE);
			}
		};

		TimerFactoryBean timerFactoryBean = new TimerFactoryBean() {
			protected Timer createTimer(String name, boolean daemon) {
				return timer;
			}
		};
		try {
			timerFactoryBean.setScheduledTimerTasks(tasks);
			timerFactoryBean.afterPropertiesSet();
			assertTrue(timerFactoryBean.getObject() instanceof Timer);
			timerTask0.run();
			timerTask1.run();
			timerTask2.run();
		}
		finally {
			timerFactoryBean.destroy();
		}

		assertTrue("Correct Timer invocations", success.size() == 4);
		assertTrue("TimerTask0 works", timerTask0.counter == 1);
		assertTrue("TimerTask1 works", task1.counter == 1);
		assertTrue("TimerTask2 works", timerTask2.counter == 1);
	}

	public void testPlainTimerFactoryBean() {
		TimerFactoryBean tfb = new TimerFactoryBean();
		tfb.afterPropertiesSet();
		tfb.destroy();
	}


	private static class TestTimerTask extends TimerTask {

		private int counter = 0;

		public void run() {
			counter++;
		}
	}


	private static class TestRunnable implements Runnable {

		private int counter = 0;

		public void run() {
			counter++;
		}
	}

}
