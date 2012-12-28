/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scripting.config;

import java.lang.reflect.Field;

import junit.framework.TestCase;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.AbstractRefreshableTargetSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Mark Fisher
 * @author Dave Syer
 */
public class ScriptingDefaultsTests extends TestCase {

	private static final String CONFIG =
		"org/springframework/scripting/config/scriptingDefaultsTests.xml";

	private static final String PROXY_CONFIG =
		"org/springframework/scripting/config/scriptingDefaultsProxyTargetClassTests.xml";


	public void testDefaultRefreshCheckDelay() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		Advised advised = (Advised) context.getBean("testBean");
		AbstractRefreshableTargetSource targetSource =
				((AbstractRefreshableTargetSource) advised.getTargetSource());
		Field field = AbstractRefreshableTargetSource.class.getDeclaredField("refreshCheckDelay");
		field.setAccessible(true);
		long delay = ((Long) field.get(targetSource)).longValue();
		assertEquals(5000L, delay);
	}

	public void testDefaultInitMethod() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("testBean");
		assertTrue(testBean.isInitialized());
	}

	public void testNameAsAlias() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("/url");
		assertTrue(testBean.isInitialized());
	}

	public void testDefaultDestroyMethod() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("nonRefreshableTestBean");
		assertFalse(testBean.isDestroyed());
		context.close();
		assertTrue(testBean.isDestroyed());
	}

	public void testDefaultAutowire() {
		ApplicationContext context = new ClassPathXmlApplicationContext(CONFIG);
		ITestBean testBean = (ITestBean) context.getBean("testBean");
		ITestBean otherBean = (ITestBean) context.getBean("otherBean");
		assertEquals(otherBean, testBean.getOtherBean());
	}

	public void testDefaultProxyTargetClass() {
		ApplicationContext context = new ClassPathXmlApplicationContext(PROXY_CONFIG);
		Object testBean = context.getBean("testBean");
		assertTrue(AopUtils.isCglibProxy(testBean));
	}

}
