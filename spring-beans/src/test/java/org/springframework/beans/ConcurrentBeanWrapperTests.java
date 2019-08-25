/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Guillaume Poirier
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 08.03.2004
 */
public class ConcurrentBeanWrapperTests {

	private final Log logger = LogFactory.getLog(getClass());

	private Set<TestRun> set = Collections.synchronizedSet(new HashSet<TestRun>());

	private Throwable ex = null;

	@Test
	public void testSingleThread() {
		for (int i = 0; i < 100; i++) {
			performSet();
		}
	}

	@Test
	public void testConcurrent() {
		for (int i = 0; i < 10; i++) {
			TestRun run = new TestRun(this);
			set.add(run);
			Thread t = new Thread(run);
			t.setDaemon(true);
			t.start();
		}
		logger.info("Thread creation over, " + set.size() + " still active.");
		synchronized (this) {
			while (!set.isEmpty() && ex == null) {
				try {
					wait();
				}
				catch (InterruptedException e) {
					logger.info(e.toString());
				}
				logger.info(set.size() + " threads still active.");
			}
		}
		if (ex != null) {
			throw new AssertionError("Unexpected exception", ex);
		}
	}

	private static void performSet() {
		TestBean bean = new TestBean();

		Properties p = (Properties) System.getProperties().clone();

		assertThat(p.size() != 0).as("The System properties must not be empty").isTrue();

		for (Iterator<?> i = p.entrySet().iterator(); i.hasNext();) {
			i.next();
			if (Math.random() > 0.9) {
				i.remove();
			}
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			p.store(buffer, null);
		}
		catch (IOException e) {
			// ByteArrayOutputStream does not throw
			// any IOException
		}
		String value = new String(buffer.toByteArray());

		BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
		wrapper.setPropertyValue("properties", value);
		assertThat(bean.getProperties()).isEqualTo(p);
	}


	private static class TestRun implements Runnable {

		private ConcurrentBeanWrapperTests test;

		public TestRun(ConcurrentBeanWrapperTests test) {
			this.test = test;
		}

		@Override
		public void run() {
			try {
				for (int i = 0; i < 100; i++) {
					performSet();
				}
			}
			catch (Throwable e) {
				test.ex = e;
			}
			finally {
				synchronized (test) {
					test.set.remove(this);
					test.notifyAll();
				}
			}
		}
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		private Properties properties;

		public Properties getProperties() {
			return properties;
		}

		public void setProperties(Properties properties) {
			this.properties = properties;
		}
	}

}
