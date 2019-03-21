/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.scripting.groovy;

import org.junit.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * @author Dave Syer
 * @author Sam Brannen
 */
public class GroovyAspectTests {

	private final LogUserAdvice logAdvice = new LogUserAdvice();

	private final GroovyScriptFactory scriptFactory = new GroovyScriptFactory("GroovyServiceImpl.grv");


	@Test
	public void manualGroovyBeanWithUnconditionalPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		testAdvice(new DefaultPointcutAdvisor(logAdvice), logAdvice, target, "GroovyServiceImpl");
	}

	@Test
	public void manualGroovyBeanWithStaticPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("execution(* %s.TestService+.*(..))", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", true);
	}

	@Test
	public void manualGroovyBeanWithDynamicPointcut() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", false);
	}

	@Test
	public void manualGroovyBeanWithDynamicPointcutProxyTargetClass() throws Exception {
		TestService target = (TestService) scriptFactory.getScriptedObject(new ResourceScriptSource(
				new ClassPathResource("GroovyServiceImpl.grv", getClass())));

		AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
		pointcut.setExpression(String.format("@within(%s.Log)", ClassUtils.getPackageName(getClass())));
		testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "GroovyServiceImpl", true);
	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message)
			throws Exception {

		testAdvice(advisor, logAdvice, target, message, false);
	}

	private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message,
			boolean proxyTargetClass) throws Exception {

		logAdvice.reset();

		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(proxyTargetClass);
		factory.addAdvisor(advisor);
		TestService bean = (TestService) factory.getProxy();

		assertEquals(0, logAdvice.getCountThrows());
		try {
			bean.sayHello();
			fail("Expected exception");
		}
		catch (TestException ex) {
			assertEquals(message, ex.getMessage());
		}
		assertEquals(1, logAdvice.getCountThrows());
	}

}
