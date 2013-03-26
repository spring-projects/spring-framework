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

package org.springframework.scripting.jruby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Messenger;
import org.springframework.tests.aop.advice.CountingBeforeAdvice;
import org.springframework.util.MBeanTestUtils;

/**
 * @author Rob Harrop
 * @author Chris Beams
 */
public final class AdvisedJRubyScriptFactoryTests {

	private static final Class<?> CLASS = AdvisedJRubyScriptFactoryTests.class;
	private static final String CLASSNAME = CLASS.getSimpleName();

	private static final String FACTORYBEAN_CONTEXT = CLASSNAME + "-factoryBean.xml";
	private static final String APC_CONTEXT = CLASSNAME + "-beanNameAutoProxyCreator.xml";

	@After
	public void resetMBeanServers() throws Exception {
		MBeanTestUtils.resetMBeanServers();
	}

	@Test
	public void testAdviseWithProxyFactoryBean() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(FACTORYBEAN_CONTEXT, CLASS);
		try {
			Messenger bean = (Messenger) ctx.getBean("messenger");
			assertTrue("Bean is not a proxy", AopUtils.isAopProxy(bean));
			assertTrue("Bean is not an Advised object", bean instanceof Advised);

			CountingBeforeAdvice advice = (CountingBeforeAdvice) ctx.getBean("advice");
			assertEquals(0, advice.getCalls());
			bean.getMessage();
			assertEquals(1, advice.getCalls());
		} finally {
			ctx.close();
		}
	}

	@Test
	public void testAdviseWithBeanNameAutoProxyCreator() {
		ClassPathXmlApplicationContext ctx =
			new ClassPathXmlApplicationContext(APC_CONTEXT, CLASS);
		try {
			Messenger bean = (Messenger) ctx.getBean("messenger");
			assertTrue("Bean is not a proxy", AopUtils.isAopProxy(bean));
			assertTrue("Bean is not an Advised object", bean instanceof Advised);

			CountingBeforeAdvice advice = (CountingBeforeAdvice) ctx.getBean("advice");
			assertEquals(0, advice.getCalls());
			bean.getMessage();
			assertEquals(1, advice.getCalls());
		} finally {
			ctx.close();
		}
	}

}
