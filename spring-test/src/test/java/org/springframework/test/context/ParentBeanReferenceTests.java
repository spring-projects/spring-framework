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

package org.springframework.test.context;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.JUnitCoreUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * Test parent application context reference from child application context.
 *
 * Child application context has a bean definition that references a bean that is defined in the parent application
 * context.
 *
 * @author Tadaya Tsuyukubo
 * @since 3.2
 */

public class ParentBeanReferenceTests {

	@Test
	public void testBeanReference() {
		JUnitCore jUnitCore = new JUnitCore();
		Result testResult = jUnitCore.run(ReferenceTests.class);
		JUnitCoreUtils.verifyResult(testResult);
	}

	@RunWith(SpringJUnit4ClassRunner.class)
	@ParentContextConfiguration("ParentBeanReferenceTests-parent-context.xml")
	@ContextConfiguration("ParentBeanReferenceTests-context.xml")
	public static final class ReferenceTests {

		@Autowired
		private ApplicationContext applicationContext;

		@Test
		public void testBeanReference() {
			assertNotNull("applicationContext must be injected.", applicationContext);

			assertTrue("ChildBean must exist in child app context.", applicationContext.containsLocalBean("childBean"));
			assertFalse("ParentBean must NOT exist in child app context.",
					applicationContext.containsLocalBean("parentBean"));

			ApplicationContext parentContext = applicationContext.getParent();
			assertNotNull("Parent App Context must exist", parentContext);
			assertFalse("ChildBean must NOT exist in parent app context.",
					parentContext.containsLocalBean("childBean"));
			assertTrue("ParentBean must exist in parent app context.", parentContext.containsLocalBean("parentBean"));

			ChildBean childBean = applicationContext.getBean("childBean", ChildBean.class);
			ParentBean parentBean = parentContext.getBean("parentBean", ParentBean.class);
			assertSame("Child bean must have injected parent bean.", parentBean, childBean.parentBean);

		}

	}

	/** defined in ParentBeanReferenceTests-parent-context.xml. */
	public static final class ParentBean {

	}

	/** defined in ParentBeanReferenceTests-context.xml. */
	public static final class ChildBean {

		public ParentBean parentBean;

		public void setParentBean(ParentBean parentBean) {
			this.parentBean = parentBean;
		}
	}

}
