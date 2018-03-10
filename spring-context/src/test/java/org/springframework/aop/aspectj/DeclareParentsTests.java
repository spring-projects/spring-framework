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

package org.springframework.aop.aspectj;

import org.junit.Before;
import org.junit.Test;
import test.mixin.Lockable;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.tests.sample.beans.ITestBean;

import static org.junit.Assert.*;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class DeclareParentsTests {

	private ITestBean testBeanProxy;

	private ApplicationContext ctx;

	@Before
	public void setUp() throws Exception {
		ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertTrue(AopUtils.isAopProxy(testBeanProxy));
	}

	@Test
	public void testIntroductionWasMade() {
		assertTrue("Introduction must have been made", testBeanProxy instanceof Lockable);
	}

	// TODO if you change type pattern from org.springframework.beans..*
	// to org.springframework..* it also matches introduction.
	// Perhaps generated advisor bean definition could be made to depend
	// on the introduction, in which case this would not be a problem.
	@Test
	public void testLockingWorks() {
		Assume.group(TestGroup.LONG_RUNNING);

		Object introductionObject = ctx.getBean("introduction");
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


class NonAnnotatedMakeLockable {

	public void checkNotLocked(Lockable mixin) {
		if (mixin.locked()) {
			throw new IllegalStateException("locked");
		}
	}

}