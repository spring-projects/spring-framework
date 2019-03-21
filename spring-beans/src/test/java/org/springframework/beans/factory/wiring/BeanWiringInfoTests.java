/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.beans.factory.wiring;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the BeanWiringInfo class.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
public final class BeanWiringInfoTests {

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithNullBeanName() throws Exception {
		new BeanWiringInfo(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithWhitespacedBeanName() throws Exception {
		new BeanWiringInfo("   \t");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithEmptyBeanName() throws Exception {
		new BeanWiringInfo("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithNegativeIllegalAutowiringValue() throws Exception {
		new BeanWiringInfo(-1, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithPositiveOutOfRangeAutowiringValue() throws Exception {
		new BeanWiringInfo(123871, true);
	}

	@Test
	public void usingAutowireCtorIndicatesAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertTrue(info.indicatesAutowiring());
	}

	@Test
	public void usingBeanNameCtorDoesNotIndicateAutowiring() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo("fooService");
		assertFalse(info.indicatesAutowiring());
	}

	@Test
	public void noDependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_NAME, true);
		assertTrue(info.getDependencyCheck());
	}

	@Test
	public void dependencyCheckValueIsPreserved() throws Exception {
		BeanWiringInfo info = new BeanWiringInfo(BeanWiringInfo.AUTOWIRE_BY_TYPE, false);
		assertFalse(info.getDependencyCheck());
	}

}
