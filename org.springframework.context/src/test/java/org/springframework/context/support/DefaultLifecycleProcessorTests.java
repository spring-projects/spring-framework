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

package org.springframework.context.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Test;

import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;

/**
 * @author Mark Fisher
 * @since 3.0
 */
public class DefaultLifecycleProcessorTests {

	@Test
	public void smartLifecycleGroupShutdown() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		TestSmartLifecycleBean bean1 = new TestSmartLifecycleBean(1, 300, stoppedBeans);
		TestSmartLifecycleBean bean2 = new TestSmartLifecycleBean(3, 100, stoppedBeans);
		TestSmartLifecycleBean bean3 = new TestSmartLifecycleBean(1, 600, stoppedBeans);
		TestSmartLifecycleBean bean4 = new TestSmartLifecycleBean(2, 400, stoppedBeans);
		TestSmartLifecycleBean bean5 = new TestSmartLifecycleBean(2, 700, stoppedBeans);
		TestSmartLifecycleBean bean6 = new TestSmartLifecycleBean(Integer.MAX_VALUE, 200, stoppedBeans);
		TestSmartLifecycleBean bean7 = new TestSmartLifecycleBean(3, 200, stoppedBeans);
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean3", bean3);
		context.getBeanFactory().registerSingleton("bean4", bean4);
		context.getBeanFactory().registerSingleton("bean5", bean5);
		context.getBeanFactory().registerSingleton("bean6", bean6);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.refresh();
		context.stop();
		assertEquals(1, getShutdownOrder(stoppedBeans.get(0)));
		assertEquals(1, getShutdownOrder(stoppedBeans.get(1)));
		assertEquals(2, getShutdownOrder(stoppedBeans.get(2)));
		assertEquals(2, getShutdownOrder(stoppedBeans.get(3)));
		assertEquals(3, getShutdownOrder(stoppedBeans.get(4)));
		assertEquals(3, getShutdownOrder(stoppedBeans.get(5)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(6)));
	}

	@Test
	public void singleSmartLifecycleShutdown() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		TestSmartLifecycleBean bean = new TestSmartLifecycleBean(99, 300, stoppedBeans);
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean", bean);
		context.refresh();
		assertTrue(bean.isRunning());
		context.stop();
		assertEquals(1, stoppedBeans.size());
		assertFalse(bean.isRunning());
		assertEquals(bean, stoppedBeans.get(0));
	}

	@Test
	public void singleLifecycleShutdown() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		Lifecycle bean = new TestLifecycleBean(stoppedBeans); 
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean", bean);
		context.refresh();
		assertFalse(bean.isRunning());
		bean.start();
		assertTrue(bean.isRunning());
		context.stop();
		assertEquals(1, stoppedBeans.size());
		assertFalse(bean.isRunning());
		assertEquals(bean, stoppedBeans.get(0));
	}

	@Test
	public void mixedShutdown() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		Lifecycle bean1 = new TestLifecycleBean(stoppedBeans);
		Lifecycle bean2 = new TestSmartLifecycleBean(500, 200, stoppedBeans);
		Lifecycle bean3 = new TestSmartLifecycleBean(Integer.MAX_VALUE, 100, stoppedBeans);
		Lifecycle bean4 = new TestLifecycleBean(stoppedBeans);
		Lifecycle bean5 = new TestSmartLifecycleBean(1, 200, stoppedBeans);
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean3", bean3);
		context.getBeanFactory().registerSingleton("bean4", bean4);
		context.getBeanFactory().registerSingleton("bean5", bean5);
		context.refresh();
		assertFalse(bean1.isRunning());
		assertFalse(bean4.isRunning());
		bean1.start();
		bean4.start();
		assertTrue(bean1.isRunning());
		assertTrue(bean2.isRunning());
		assertTrue(bean3.isRunning());
		assertTrue(bean4.isRunning());
		assertTrue(bean5.isRunning());
		context.stop();
		assertFalse(bean1.isRunning());
		assertFalse(bean2.isRunning());
		assertFalse(bean3.isRunning());
		assertFalse(bean4.isRunning());
		assertFalse(bean5.isRunning());
		assertEquals(5, stoppedBeans.size());
		assertEquals(1, getShutdownOrder(stoppedBeans.get(0)));
		assertEquals(500, getShutdownOrder(stoppedBeans.get(1)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(2)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(3)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(4)));
	}

	@Test
	public void dependantShutdownFirstEvenIfItsOrderIsHigher() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		TestSmartLifecycleBean bean1 = new TestSmartLifecycleBean(1, 200, stoppedBeans);
		TestSmartLifecycleBean bean99 = new TestSmartLifecycleBean(99, 100, stoppedBeans);
		TestSmartLifecycleBean bean2 = new TestSmartLifecycleBean(2, 300, stoppedBeans);
		TestSmartLifecycleBean bean7 = new TestSmartLifecycleBean(7, 400, stoppedBeans);
		TestSmartLifecycleBean beanLast = new TestSmartLifecycleBean(Integer.MAX_VALUE, 400, stoppedBeans);
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("bean99", bean99);
		context.getBeanFactory().registerSingleton("beanLast", beanLast);
		context.getBeanFactory().registerDependentBean("bean2", "bean99");
		context.refresh();
		assertTrue(bean1.isRunning());
		assertTrue(bean7.isRunning());
		assertTrue(bean99.isRunning());
		context.stop();
		assertFalse(bean1.isRunning());
		assertFalse(bean7.isRunning());
		assertFalse(bean99.isRunning());
		assertEquals(5, stoppedBeans.size());
		assertEquals(1, getShutdownOrder(stoppedBeans.get(0)));
		assertEquals(99, getShutdownOrder(stoppedBeans.get(1)));
		assertEquals(2, getShutdownOrder(stoppedBeans.get(2)));
		assertEquals(7, getShutdownOrder(stoppedBeans.get(3)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(4)));
	}

	@Test
	public void dependantShutdownFirstEvenIfNotSmartLifecycle() throws Exception {
		CopyOnWriteArrayList<Lifecycle> stoppedBeans = new CopyOnWriteArrayList<Lifecycle>();
		TestSmartLifecycleBean bean1 = new TestSmartLifecycleBean(1, 200, stoppedBeans);
		TestLifecycleBean simpleBean = new TestLifecycleBean(stoppedBeans);
		TestSmartLifecycleBean bean2 = new TestSmartLifecycleBean(2, 300, stoppedBeans);
		TestSmartLifecycleBean bean7 = new TestSmartLifecycleBean(7, 400, stoppedBeans);
		TestSmartLifecycleBean beanLast = new TestSmartLifecycleBean(Integer.MAX_VALUE, 400, stoppedBeans);
		StaticApplicationContext context = new StaticApplicationContext();
		context.getBeanFactory().registerSingleton("bean1", bean1);
		context.getBeanFactory().registerSingleton("bean2", bean2);
		context.getBeanFactory().registerSingleton("bean7", bean7);
		context.getBeanFactory().registerSingleton("simpleBean", simpleBean);
		context.getBeanFactory().registerSingleton("beanLast", beanLast);
		context.getBeanFactory().registerDependentBean("bean2", "simpleBean");
		context.refresh();
		assertTrue(bean1.isRunning());
		assertTrue(bean7.isRunning());
		assertFalse(simpleBean.isRunning());
		simpleBean.start();
		assertTrue(simpleBean.isRunning());
		context.stop();
		assertFalse(bean1.isRunning());
		assertFalse(bean7.isRunning());
		assertFalse(simpleBean.isRunning());
		assertEquals(5, stoppedBeans.size());
		assertEquals(1, getShutdownOrder(stoppedBeans.get(0)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(1)));
		assertEquals(2, getShutdownOrder(stoppedBeans.get(2)));
		assertEquals(7, getShutdownOrder(stoppedBeans.get(3)));
		assertEquals(Integer.MAX_VALUE, getShutdownOrder(stoppedBeans.get(4)));
	}


	private static int getShutdownOrder(Lifecycle lifecycle) {
		return (lifecycle instanceof SmartLifecycle) ?
				((SmartLifecycle) lifecycle).getShutdownOrder() : Integer.MAX_VALUE;
	}


	private class TestLifecycleBean implements Lifecycle {

		protected final CopyOnWriteArrayList<Lifecycle> stoppedBeans;

		private volatile boolean running;


		TestLifecycleBean(CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			this.stoppedBeans = stoppedBeans;
		}

		public boolean isRunning() {
			return this.running;
		}

		public void start() {
			this.running = true;
		}

		public void stop() {
			this.stoppedBeans.add(this);
			this.running = false;
		}

	}


	private class TestSmartLifecycleBean extends TestLifecycleBean implements SmartLifecycle {

		private final int shutdownOrder;

		private final int shutdownDelay;


		TestSmartLifecycleBean(int shutdownOrder, int shutdownDelay, CopyOnWriteArrayList<Lifecycle> stoppedBeans) {
			super(stoppedBeans);
			this.shutdownOrder = shutdownOrder;
			this.shutdownDelay = shutdownDelay;
		}

		public int getShutdownOrder() {
			return this.shutdownOrder;
		}

		public boolean isAutoStartup() {
			return true;
		}

		public void stop(final Runnable callback) {
			final int delay = this.shutdownDelay;
			new Thread(new Runnable() {
				public void run() {
					try {
						Thread.sleep(delay);
						stop();
					}
					catch (InterruptedException e) {
						// ignore
					}
					finally {
						callback.run();
					}
				}
			}).start();
		}
	}

}
