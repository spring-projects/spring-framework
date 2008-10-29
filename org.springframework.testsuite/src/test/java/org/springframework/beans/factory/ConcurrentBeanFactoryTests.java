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

package org.springframework.beans.factory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Guillaume Poirier
 * @author Juergen Hoeller
 * @since 10.03.2004
 */
public class ConcurrentBeanFactoryTests extends TestCase {

	private static final Log logger = LogFactory.getLog(ConcurrentBeanFactoryTests.class);

	private static final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");

	private static final Date date1;

	private static final Date date2;

	static {
		try {
			date1 = df.parse("2004/08/08");
			date2 = df.parse("2000/02/02");
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private BeanFactory factory;

	private Set set = Collections.synchronizedSet(new HashSet());

	private Throwable ex = null;

	protected void setUp() throws Exception {
		XmlBeanFactory factory = new XmlBeanFactory(new ClassPathResource("concurrent.xml", getClass()));
		CustomDateEditor editor = new CustomDateEditor(df, false);
		factory.registerCustomEditor(Date.class, editor);
		this.factory = factory;
	}

	public void testSingleThread() {
		for (int i = 0; i < 100; i++) {
			performTest();
		}
	}

	public void testConcurrent() {
		for (int i = 0; i < 100; i++) {
			TestRun run = new TestRun();
			run.setDaemon(true);
			set.add(run);
		}
		for (Iterator it = new HashSet(set).iterator(); it.hasNext();) {
			TestRun run = (TestRun) it.next();
			run.start();
		}
		logger.info("Thread creation over, " + set.size() + " still active.");
		synchronized (set) {
			while (!set.isEmpty() && ex == null) {
				try {
					set.wait();
				}
				catch (InterruptedException e) {
					logger.info(e.toString());
				}
				logger.info(set.size() + " threads still active.");
			}
		}
		if (ex != null) {
			fail(ex.getMessage());
		}
	}

	private void performTest() {
		ConcurrentBean b1 = (ConcurrentBean) factory.getBean("bean1");
		ConcurrentBean b2 = (ConcurrentBean) factory.getBean("bean2");

		assertEquals(b1.getDate(), date1);
		assertEquals(b2.getDate(), date2);
	}


	private class TestRun extends Thread {

		public void run() {
			try {
				for (int i = 0; i < 10000; i++) {
					performTest();
				}
			}
			catch (Throwable e) {
				ex = e;
			}
			finally {
				synchronized (set) {
					set.remove(this);
					set.notifyAll();
				}
			}
		}
	}


	public static class ConcurrentBean {

		private Date date;

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

}
