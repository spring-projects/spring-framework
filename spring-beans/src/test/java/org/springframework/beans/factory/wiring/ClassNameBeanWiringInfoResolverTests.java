/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the ClassNameBeanWiringInfoResolver class.
 *
 * @author Rick Evans
 */
public final class ClassNameBeanWiringInfoResolverTests {

	@Test(expected = IllegalArgumentException.class)
	public void resolveWiringInfoWithNullBeanInstance() throws Exception {
		new ClassNameBeanWiringInfoResolver().resolveWiringInfo(null);
	}

	@Test
	public void resolveWiringInfo() {
		ClassNameBeanWiringInfoResolver resolver = new ClassNameBeanWiringInfoResolver();
		Long beanInstance = new Long(1);
		BeanWiringInfo info = resolver.resolveWiringInfo(beanInstance);
		assertNotNull(info);
		assertEquals("Not resolving bean name to the class name of the supplied bean instance as per class contract.",
				beanInstance.getClass().getName(), info.getBeanName());
	}

}
