/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.aspectj.AdviceBindingTestAspect.AdviceBindingCollaborator;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for various parameter binding scenarios with before advice.
 *
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Chris Beams
 */
class BeforeAdviceBindingTests {

	private ClassPathXmlApplicationContext ctx;

	private AdviceBindingCollaborator mockCollaborator = mock();

	private ITestBean testBeanProxy;

	private TestBean testBeanTarget;


	@BeforeEach
	void setup() throws Exception {
		this.ctx = new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());

		testBeanProxy = (ITestBean) ctx.getBean("testBean");
		assertThat(AopUtils.isAopProxy(testBeanProxy)).isTrue();

		// we need the real target too, not just the proxy...
		testBeanTarget = (TestBean) ((Advised) testBeanProxy).getTargetSource().getTarget();

		AdviceBindingTestAspect beforeAdviceAspect = (AdviceBindingTestAspect) ctx.getBean("testAspect");

		beforeAdviceAspect.setCollaborator(mockCollaborator);
	}

	@AfterEach
	void tearDown() {
		this.ctx.close();
	}



	@Test
	void oneIntArg() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntArg(5);
	}

	@Test
	void oneObjectArgBoundToProxyUsingThis() {
		testBeanProxy.getAge();
		verify(mockCollaborator).oneObjectArg(this.testBeanProxy);
	}

	@Test
	void oneIntAndOneObjectArgs() {
		testBeanProxy.setAge(5);
		verify(mockCollaborator).oneIntAndOneObject(5,this.testBeanTarget);
	}

	@Test
	void needsJoinPoint() {
		testBeanProxy.getAge();
		verify(mockCollaborator).needsJoinPoint("getAge");
	}

	@Test
	void needsJoinPointStaticPart() {
		testBeanProxy.getAge();
		verify(mockCollaborator).needsJoinPointStaticPart("getAge");
	}

}
