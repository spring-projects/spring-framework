/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.*;
import static test.util.TestResourceUtils.qualifiedResource;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.core.io.Resource;

/**
 * @author Guillaume Poirier
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.03.2004
 */
public final class ConcurrentBeanFactoryTests {

	private static final Log logger = LogFactory.getLog(ConcurrentBeanFactoryTests.class);
	private static final Resource CONTEXT = qualifiedResource(ConcurrentBeanFactoryTests.class, "context.xml");

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
	private static final Date DATE_1, DATE_2;

	static {
		try {
			DATE_1 = DATE_FORMAT.parse("2004/08/08");
			DATE_2 = DATE_FORMAT.parse("2000/02/02");
		}
		catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

	private BeanFactory factory;

	private final Set<TestRun> set = Collections.synchronizedSet(new HashSet<TestRun>());

	private Throwable ex = null;

	@Before
	public void setUp() throws Exception {
		XmlBeanFactory factory = new XmlBeanFactory(CONTEXT);
		factory.addPropertyEditorRegistrar(new PropertyEditorRegistrar() {
			@Override
			public void registerCustomEditors(PropertyEditorRegistry registry) {
				registry.registerCustomEditor(Date.class, new CustomDateEditor((DateFormat) DATE_FORMAT.clone(), false));
			}
		});
		this.factory = factory;
	}

	@Test
	public void testSingleThread() {
		for (int i = 0; i < 100; i++) {
			performTest();
		}
	}

	@Test
	public void testConcurrent() {
		for (int i = 0; i < 100; i++) {
			TestRun run = new TestRun();
			run.setDaemon(true);
			set.add(run);
		}
		for (Iterator<TestRun> it = new HashSet<TestRun>(set).iterator(); it.hasNext();) {
			TestRun run = it.next();
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

		assertEquals(b1.getDate(), DATE_1);
		assertEquals(b2.getDate(), DATE_2);
	}


	private class TestRun extends Thread {

		@Override
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
