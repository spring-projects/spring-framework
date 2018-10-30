/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.scripting.support;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Messenger;

import static org.junit.Assert.*;

/**
 * {@link StandardScriptFactory} (lang:std) tests for JavaScript.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class StandardScriptFactoryTests {

	@Test
	public void testJsr223FromTagWithInterface() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithInterface"));
		Messenger messenger = (Messenger) ctx.getBean("messengerWithInterface");
		assertFalse(AopUtils.isAopProxy(messenger));
		assertEquals("Hello World!", messenger.getMessage());
	}

	@Test
	public void testRefreshableJsr223FromTagWithInterface() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("refreshableMessengerWithInterface"));
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessengerWithInterface");
		assertTrue(AopUtils.isAopProxy(messenger));
		assertTrue(messenger instanceof Refreshable);
		assertEquals("Hello World!", messenger.getMessage());
	}

	@Test
	public void testInlineJsr223FromTagWithInterface() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jsr223-with-xsd.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("inlineMessengerWithInterface"));
		Messenger messenger = (Messenger) ctx.getBean("inlineMessengerWithInterface");
		assertFalse(AopUtils.isAopProxy(messenger));
		assertEquals("Hello World!", messenger.getMessage());
	}

}
