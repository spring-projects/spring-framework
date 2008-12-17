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

package org.springframework.scripting.jruby;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.CountingBeforeAdvice;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Messenger;

/**
 * @author Rob Harrop
 */
public class AdvisedJRubyScriptFactoryTests extends TestCase {

	public void testAdviseWithProxyFactoryBean() throws Exception {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("advisedByProxyFactoryBean.xml", getClass());

		Messenger bean = (Messenger) context.getBean("messenger");
		assertTrue("Bean is not a proxy", AopUtils.isAopProxy(bean));
		assertTrue("Bean is not an Advised object", bean instanceof Advised);

		CountingBeforeAdvice advice = (CountingBeforeAdvice) context.getBean("advice");
		assertEquals(0, advice.getCalls());
		bean.getMessage();
		assertEquals(1, advice.getCalls());
	}

	public void testAdviseWithBeanNameAutoProxyCreator() throws Exception {
		ApplicationContext context =
				new ClassPathXmlApplicationContext("advisedByBeanNameAutoProxyCreator.xml", getClass());

		Messenger bean = (Messenger) context.getBean("messenger");
		assertTrue("Bean is not a proxy", AopUtils.isAopProxy(bean));
		assertTrue("Bean is not an Advised object", bean instanceof Advised);

		CountingBeforeAdvice advice = (CountingBeforeAdvice) context.getBean("advice");
		assertEquals(0, advice.getCalls());
		bean.getMessage();
		assertEquals(1, advice.getCalls());
	}

}
