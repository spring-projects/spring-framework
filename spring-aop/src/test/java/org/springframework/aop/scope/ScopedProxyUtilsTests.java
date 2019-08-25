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

package org.springframework.aop.scope;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ScopedProxyUtils}.
 *
 * @author Sam Brannen
 * @since 5.1.10
 */
public class ScopedProxyUtilsTests {

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void getTargetBeanNameAndIsScopedTarget() {
		String originalBeanName = "myBean";
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);

		assertNotEquals(originalBeanName, targetBeanName);
		assertTrue(targetBeanName.endsWith(originalBeanName));
		assertTrue(ScopedProxyUtils.isScopedTarget(targetBeanName));
		assertFalse(ScopedProxyUtils.isScopedTarget(originalBeanName));
	}

	@Test
	public void getOriginalBeanNameAndIsScopedTarget() {
		String originalBeanName = "myBean";
		String targetBeanName = ScopedProxyUtils.getTargetBeanName(originalBeanName);
		String parsedOriginalBeanName = ScopedProxyUtils.getOriginalBeanName(targetBeanName);

		assertNotEquals(targetBeanName, parsedOriginalBeanName);
		assertEquals(originalBeanName, parsedOriginalBeanName);
		assertTrue(ScopedProxyUtils.isScopedTarget(targetBeanName));
		assertFalse(ScopedProxyUtils.isScopedTarget(parsedOriginalBeanName));
	}

	@Test
	public void getOriginalBeanNameForNullTargetBean() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("bean name 'null' does not refer to the target of a scoped proxy");
		ScopedProxyUtils.getOriginalBeanName(null);
	}

	@Test
	public void getOriginalBeanNameForNonScopedTarget() {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("bean name 'myBean' does not refer to the target of a scoped proxy");
		ScopedProxyUtils.getOriginalBeanName("myBean");
	}

}
