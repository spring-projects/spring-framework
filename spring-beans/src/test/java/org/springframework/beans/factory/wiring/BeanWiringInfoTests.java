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

package org.springframework.beans.factory.wiring;

import junit.framework.TestCase;

/**
 * Unit tests for the BeanWiringInfo class.
 *
 * @author Rick Evans
 */
public final class BeanWiringInfoTests extends TestCase {

	public void testCtorWithNullBeanName() throws Exception {
		try {
			new BeanWiringInfo(null);
			fail("Must have thrown an IllegalArgumentException by this point (null argument).");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	public void testCtorWithWhitespacedBeanName() throws Exception {
		try {
			new BeanWiringInfo("   \t");
			fail("Must have thrown an IllegalArgumentException by this point (bean name has only whitespace).");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	public void testCtorWithEmptyBeanName() throws Exception {
		try {
			new BeanWiringInfo("");
			fail("Must have thrown an IllegalArgumentException by this point (bean name is empty).");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	public void testCtorWithNegativeIllegalAutowiringValue() throws Exception {
		try {
			new BeanWiringInfo(-1, true);
			fail("Must have thrown an IllegalArgumentException by this point (out-of-range argument).");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	public void testCtorWithPositiveOutOfRangeAutowiringValue() throws Exception {
		try {
			new BeanWiringInfo(123871, true);
			fail("Must have thrown an IllegalArgumentException by this point (out-of-range argument).");
		}
		catch (IllegalArgumentException ex) {
		}
	}

	public void testUsingAutowireCtorIndicatesAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertTrue(info.indicatesAutowiring());
	}

	public void testUsingBeanNameCtorDoesNotIndicateAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo("fooService");
		assertFalse(info.indicatesAutowiring());
	}

	public void testNoDependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertTrue(info.getDependencyCheck());
	}

	public void testDependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false);
		assertFalse(info.getDependencyCheck());
	}

}
