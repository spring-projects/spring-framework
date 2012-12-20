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
package org.springframework.aop.support;

import junit.framework.TestCase;

import org.springframework.aop.framework.ProxyFactory;
import test.beans.TestBean;
import org.springframework.util.ClassUtils;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 */
public class ClassUtilsTests extends TestCase {

	public void testGetShortNameForCglibClass() {
		TestBean tb = new TestBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(tb);
		pf.setProxyTargetClass(true);
		TestBean proxy = (TestBean) pf.getProxy();
		String className = ClassUtils.getShortName(proxy.getClass());
		assertEquals("Class name did not match", "TestBean", className);
	}
}
