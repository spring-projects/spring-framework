/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.aspectj.generic;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for AspectJ pointcut expression matching when working with bridge methods.
 *
 * <p>This class focuses on class proxying.
 *
 * <p>See GenericBridgeMethodMatchingTests for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
public final class GenericBridgeMethodMatchingClassProxyTests extends GenericBridgeMethodMatchingTests {

	@Test
	public void testGenericDerivedInterfaceMethodThroughClass() {
		((DerivedStringParameterizedClass) testBean).genericDerivedInterfaceMethod("");
		assertEquals(1, counterAspect.count);
	}

	@Test
	public void testGenericBaseInterfaceMethodThroughClass() {
		((DerivedStringParameterizedClass) testBean).genericBaseInterfaceMethod("");
		assertEquals(1, counterAspect.count);
	}

}
