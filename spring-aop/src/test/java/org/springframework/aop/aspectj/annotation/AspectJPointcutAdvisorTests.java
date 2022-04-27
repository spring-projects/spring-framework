/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.junit.jupiter.api.Test;
import test.aop.PerTargetAspect;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcutTests;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AspectJPointcutAdvisorTests {

	private final AspectJAdvisorFactory af = new ReflectiveAspectJAdvisorFactory();


	@Test
	public void testSingleton() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);

		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
				ajexp, TestBean.class.getMethod("getAge"), af,
				new SingletonMetadataAwareAspectInstanceFactory(new ExceptionThrowingAspect(null), "someBean"),
				1, "someBean");

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isSameAs(Pointcut.TRUE);
		assertThat(ajpa.isPerInstance()).isFalse();
	}

	@Test
	public void testPerTarget() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(AspectJExpressionPointcutTests.MATCH_ALL_METHODS);

		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
				ajexp, TestBean.class.getMethod("getAge"), af,
				new SingletonMetadataAwareAspectInstanceFactory(new PerTargetAspect(), "someBean"),
				1, "someBean");

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
		boolean condition = ajpa.getAspectMetadata().getPerClausePointcut() instanceof AspectJExpressionPointcut;
		assertThat(condition).isTrue();
		assertThat(ajpa.isPerInstance()).isTrue();

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getClassFilter().matches(TestBean.class)).isTrue();
		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getAge"), TestBean.class)).isFalse();

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getSpouse"), TestBean.class)).isTrue();
	}

	@Test
	public void testPerCflowTarget() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowAspect.class));
	}

	@Test
	public void testPerCflowBelowTarget() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowBelowAspect.class));
	}

	private void testIllegalInstantiationModel(Class<?> c) throws AopConfigException {
		new AspectMetadata(c, "someBean");
	}

}
