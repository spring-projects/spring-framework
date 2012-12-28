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

package org.springframework.aop.scope;

import static org.junit.Assert.assertSame;
import static test.util.TestResourceUtils.qualifiedResource;

import org.junit.Test;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.Resource;

/**
 * @author Mark Fisher
 * @author Chris Beams
 */
public final class ScopedProxyAutowireTests {

	private static final Class<?> CLASS = ScopedProxyAutowireTests.class;

	private static final Resource SCOPED_AUTOWIRE_TRUE_CONTEXT = qualifiedResource(CLASS, "scopedAutowireTrue.xml");
	private static final Resource SCOPED_AUTOWIRE_FALSE_CONTEXT = qualifiedResource(CLASS, "scopedAutowireFalse.xml");

	@Test
	public void testScopedProxyInheritsAutowireCandidateFalse() {
		XmlBeanFactory bf = new XmlBeanFactory(SCOPED_AUTOWIRE_FALSE_CONTEXT);
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean unscoped = (TestBean) bf.getBean("unscoped");
		assertSame(unscoped, autowired.getChild());
	}

	@Test
	public void testScopedProxyReplacesAutowireCandidateTrue() {
		XmlBeanFactory bf = new XmlBeanFactory(SCOPED_AUTOWIRE_TRUE_CONTEXT);
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean scoped = (TestBean) bf.getBean("scoped");
		assertSame(scoped, autowired.getChild());
	}


	static class TestBean {

		private TestBean child;

		public void setChild(TestBean child) {
			this.child = child;
		}

		public TestBean getChild() {
			return this.child;
		}
	}

}
