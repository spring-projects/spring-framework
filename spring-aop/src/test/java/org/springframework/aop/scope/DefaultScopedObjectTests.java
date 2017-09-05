/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the {@link DefaultScopedObject} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class DefaultScopedObjectTests {

	private static final String GOOD_BEAN_NAME = "foo";


	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithNullBeanFactory() throws Exception {
		new DefaultScopedObject(null, GOOD_BEAN_NAME);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithNullTargetBeanName() throws Exception {
		testBadTargetBeanName(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithEmptyTargetBeanName() throws Exception {
		testBadTargetBeanName("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithJustWhitespacedTargetBeanName() throws Exception {
		testBadTargetBeanName("   ");
	}

	private static void testBadTargetBeanName(final String badTargetBeanName) {
		ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);
		new DefaultScopedObject(factory, badTargetBeanName);
	}

}
