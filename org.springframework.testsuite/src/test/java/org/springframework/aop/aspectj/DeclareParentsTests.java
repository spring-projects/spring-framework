/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.aop.framework.Lockable;
import org.springframework.aop.support.AopUtils;

/**
 * @author Rod Johnson
 */
public class DeclareParentsTests extends AbstractAdviceBindingTests {

	protected String getConfigPath() {
		return "declare-parents-tests.xml";
	}

	public void testIntroductionWasMade() {
		assertTrue("Introduction must have been made", testBeanProxy instanceof Lockable);
	}

	// TODO if you change type pattern from org.springframework.beans..*
	// to org.springframework..* it also matches introduction.
	// Perhaps generated advisor bean definition could be made to depend
	// on the introduction, in which case this would not be a problem.
	public void testLockingWorks() {
		Object introductionObject = applicationContext.getBean("introduction");
		assertFalse("Introduction should not be proxied", AopUtils.isAopProxy(introductionObject));

		Lockable lockable = (Lockable) testBeanProxy;
		assertFalse(lockable.locked());

		// Invoke a non-advised method
		testBeanProxy.getAge();

		testBeanProxy.setName("");
		lockable.lock();
		try {
			testBeanProxy.setName(" ");
			fail("Should be locked");
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

}
