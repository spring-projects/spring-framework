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

package org.springframework.aop.scope;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.test.AssertThrows;

/**
 * Unit tests for the {@link DefaultScopedObject} class.
 *
 * @author Rick Evans
 */
public final class DefaultScopedObjectTests extends TestCase {

	private static final String GOOD_BEAN_NAME = "foo";


	public void testCtorWithNullBeanFactory() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new DefaultScopedObject(null, GOOD_BEAN_NAME);
			}
		}.runTest();
	}

	public void testCtorWithNullTargetBeanName() throws Exception {
		testBadTargetBeanName(null);
	}

	public void testCtorWithEmptyTargetBeanName() throws Exception {
		testBadTargetBeanName("");
	}

	public void testCtorWithJustWhitespacedTargetBeanName() throws Exception {
		testBadTargetBeanName("   ");
	}


	private static void testBadTargetBeanName(final String badTargetBeanName) {
		MockControl mock = MockControl.createControl(ConfigurableBeanFactory.class);
		final ConfigurableBeanFactory factory = (ConfigurableBeanFactory) mock.getMock();
		mock.replay();

		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new DefaultScopedObject(factory, badTargetBeanName);
			}
		}.runTest();

		mock.verify();
	}

}
