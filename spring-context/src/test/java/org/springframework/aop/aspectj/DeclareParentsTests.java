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

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test.mixin.Lockable;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class DeclareParentsTests {

	private ITestBean testBeanProxy;

	private Object introductionObject;


	@BeforeEach
	public void setup() {
		ClassPathXmlApplicationContext ctx =
				new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		introductionObject = ctx.getBean("introduction");
	}


	@Test
	public void testIntroductionWasMade() {
		assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();
		assertThat(AopUtils.isAopProxy(introductionObject)).as("Introduction should not be proxied").isFalse();
		boolean condition = testBeanProxy instanceof Lockable;
		assertThat(condition).as("Introduction must have been made").isTrue();
	}

	// TODO if you change type pattern from org.springframework.beans..*
	// to org.springframework..* it also matches introduction.
	// Perhaps generated advisor bean definition could be made to depend
	// on the introduction, in which case this would not be a problem.
	@Test
	public void testLockingWorks() {
		Lockable lockable = (Lockable) testBeanProxy;
		assertThat(lockable.locked()).isFalse();

		// Invoke a non-advised method
		testBeanProxy.getAge();

		testBeanProxy.setName("");
		lockable.lock();
		assertThatIllegalStateException().as("should be locked").isThrownBy(() ->
				testBeanProxy.setName(" "));
	}

}


class NonAnnotatedMakeLockable {

	public void checkNotLocked(Lockable mixin) {
		if (mixin.locked()) {
			throw new IllegalStateException("locked");
		}
	}
}
