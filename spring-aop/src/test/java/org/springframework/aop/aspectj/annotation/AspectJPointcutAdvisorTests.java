/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.ExceptionThrowingAspect;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.aop.testfixture.aspectj.CommonExpressions;
import org.springframework.aop.testfixture.aspectj.PerTargetAspect;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
class AspectJPointcutAdvisorTests {

	private final AspectJAdvisorFactory af = new ReflectiveAspectJAdvisorFactory();


	@Test
	void singleton() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(CommonExpressions.MATCH_ALL_METHODS);

		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
				ajexp, TestBean.class.getMethod("getAge"), af,
				new SingletonMetadataAwareAspectInstanceFactory(new ExceptionThrowingAspect(null), "someBean"),
				1, "someBean");

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isSameAs(Pointcut.TRUE);
		assertThat(ajpa.isPerInstance()).isFalse();
	}

	@Test
	void perTarget() throws SecurityException, NoSuchMethodException {
		AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
		ajexp.setExpression(CommonExpressions.MATCH_ALL_METHODS);

		InstantiationModelAwarePointcutAdvisorImpl ajpa = new InstantiationModelAwarePointcutAdvisorImpl(
				ajexp, TestBean.class.getMethod("getAge"), af,
				new SingletonMetadataAwareAspectInstanceFactory(new PerTargetAspect(), "someBean"),
				1, "someBean");

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isNotSameAs(Pointcut.TRUE);
		assertThat(ajpa.getAspectMetadata().getPerClausePointcut()).isInstanceOf(AspectJExpressionPointcut.class);
		assertThat(ajpa.isPerInstance()).isTrue();

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getClassFilter().matches(TestBean.class)).isTrue();
		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getAge"), TestBean.class)).isFalse();

		assertThat(ajpa.getAspectMetadata().getPerClausePointcut().getMethodMatcher().matches(
				TestBean.class.getMethod("getSpouse"), TestBean.class)).isTrue();
	}

	@Test
	void perCflowTarget() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowAspect.class));
	}

	@Test
	void perCflowBelowTarget() {
		assertThatExceptionOfType(AopConfigException.class).isThrownBy(() ->
				testIllegalInstantiationModel(AbstractAspectJAdvisorFactoryTests.PerCflowBelowAspect.class));
	}

	private void testIllegalInstantiationModel(Class<?> c) throws AopConfigException {
		new AspectMetadata(c, "someBean");
	}

}
