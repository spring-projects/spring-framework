/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.testfixture.mixin.Lockable;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
class DeclareParentsTests {

	private ClassPathXmlApplicationContext ctx;

	private ITestBean testBeanProxy;

	private Object introductionObject;


	@BeforeEach
	void setup() {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		introductionObject = ctx.getBean("introduction");
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}


	@Test
	void introductionWasMade() {
		assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();
		assertThat(AopUtils.isAopProxy(introductionObject)).as("Introduction should not be proxied").isFalse();
		assertThat(testBeanProxy).as("Introduction must have been made").isInstanceOf(Lockable.class);
	}

	// TODO if you change type pattern from org.springframework.beans..*
	// to org.springframework..* it also matches introduction.
	// Perhaps generated advisor bean definition could be made to depend
	// on the introduction, in which case this would not be a problem.
	@Test
	void lockingWorks() {
		Lockable lockable = (Lockable) testBeanProxy;
		assertThat(lockable.locked()).isFalse();

		// Invoke a non-advised method
		testBeanProxy.getAge();

		testBeanProxy.setName("");
		lockable.lock();
		assertThatIllegalStateException().as("should be locked").isThrownBy(() -> testBeanProxy.setName(" "));
	}

}


class NonAnnotatedMakeLockable {

	public void checkNotLocked(Lockable mixin) {
		if (mixin.locked()) {
			throw new IllegalStateException("locked");
		}
	}
}
