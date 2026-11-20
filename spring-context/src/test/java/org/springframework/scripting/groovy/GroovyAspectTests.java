/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Sam Brannen
 */
class GroovyAspectTests {

	private final LogUserAdvice logAdvice = new LogUserAdvice();

	private final GroovyScriptFactory scriptFactory = new GroovyScriptFactory("GroovyServiceImpl.grv");


	@Test
	void manualGroovyBeanWithUnconditionalPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		testAdvice(new DefaultPointcutAdvisor(logAdvice), logAdvice, target, "GroovyServiceImpl");
	}

	@Test
	void manualGroovyBeanWithStaticPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("execution(* %s.TestService+.*(..))", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", true);
	}

	@Test
	void manualGroovyBeanWithDynamicPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", false);
	}

	@Test
	void manualGroovyBeanWithDynamicPointcutProxyTargetClass() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", true);
	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message) {

		testAdvice(advisor, logAdvice, target, message, false);
	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message,
			boolean proxyTargetClass) {

		logAdvice.reset();

		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(proxyTargetClass);
		factory.addAdvisor(advisor);
		TestService bean = (TestService) factory.getProxy();

		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThatExceptionOfType(TestException.class)
				.isThrownBy(bean::sayHello)
				.withMessage(message);
		assertThat(logAdvice.getCountThrows()).isEqualTo(1);
	}

}
