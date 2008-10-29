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

package org.springframework.aop.aspectj;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.ITestBean;
import org.springframework.beans.TestBean;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * @author Rod Johnson
 */
public abstract class AbstractAdviceBindingTests extends AbstractDependencyInjectionSpringContextTests {

	protected ITestBean testBeanProxy;
	
	protected TestBean testBeanTarget;

	public final void setTestBean(ITestBean injectedTestBean) throws Exception {
		assertTrue(AopUtils.isAopProxy(injectedTestBean));
		this.testBeanProxy = injectedTestBean;
		// we need the real target too, not just the proxy...
		this.testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();
	}
	
	// Simple test to ensure all is well with the XML file.
	// Note that this implicitly tests that the arg-names binding is working.
	public final void testParse() {
		// Do nothing
	}

}
