/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.TestBean;
import org.springframework.context.ApplicationContext;

/**
 * @author Arjen Poutsma
 */
public class SimpleThreadScopeTest {

	private ApplicationContext applicationContext;

	@Before
	public void setUp() {
		applicationContext = new ClassPathXmlApplicationContext("simpleThreadScopeTests.xml", getClass());
	}

	@Test
	public void getFromScope() throws Exception {
		String name = "threadScopedObject";
		TestBean bean = (TestBean) this.applicationContext.getBean(name);
		assertNotNull(bean);
		assertSame(bean, this.applicationContext.getBean(name));
		TestBean bean2 = (TestBean) this.applicationContext.getBean(name);
		assertSame(bean, bean2);
	}

	@Test
	public void getMultipleInstances() throws Exception {
		final TestBean[] beans = new TestBean[2];
		Thread thread1 = new Thread(new Runnable() {
			public void run() {
				beans[0] = applicationContext.getBean("threadScopedObject", TestBean.class);
			}
		});
		Thread thread2 = new Thread(new Runnable() {
			public void run() {
				beans[1] = applicationContext.getBean("threadScopedObject", TestBean.class);
			}
		});
		thread1.start();
		thread2.start();

		Thread.sleep(200);

		assertNotNull(beans[0]);
		assertNotNull(beans[1]);

		assertNotSame(beans[0], beans[1]);
	}

}
