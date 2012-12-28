/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.scripting.bsh;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collection;

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.beans.TestBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.TestBeanAwareMessenger;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class BshScriptFactoryTests extends TestCase {

	public void testStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());

		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator"));
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger"));

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertEquals(calc, calc);
		assertEquals(messenger, messenger);
		assertTrue(!messenger.equals(calc));
		assertTrue(messenger.hashCode() != calc.hashCode());
		assertTrue(!messenger.toString().equals(calc.toString()));

		assertEquals(5, calc.add(2, 3));

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		assertTrue(ctx.getBeansOfType(Calculator.class).values().contains(calc));
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testStaticScriptWithNullReturnValue() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfig"));

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfig");
		messenger.setMessage(null);
		assertNull(messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testStaticScriptWithTwoInterfacesSpecified() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerWithConfigExtra"));

		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerWithConfigExtra");
		messenger.setMessage(null);
		assertNull(messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));

		ctx.close();
		assertNull(messenger.getMessage());
	}

	public void testStaticWithScriptReturningInstance() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstance"));

		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");
		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));

		ctx.close();
		assertNull(messenger.getMessage());
	}

	public void testStaticScriptImplementingInterface() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerImpl"));

		Messenger messenger = (Messenger) ctx.getBean("messengerImpl");
		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));

		ctx.close();
		assertNull(messenger.getMessage());
	}

	public void testStaticPrototypeScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(messenger));
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertNotSame(messenger, messenger2);
		assertSame(messenger.getClass(), messenger2.getClass());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
	}

	public void testNonStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertTrue("Should be a proxy for refreshable scripts", AopUtils.isAopProxy(messenger));
		assertTrue("Should be an instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Message is incorrect after refresh", desiredMessage, messenger.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	public void testNonStaticPrototypeScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bshRefreshableContext.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertTrue("Should be a proxy for refreshable scripts", AopUtils.isAopProxy(messenger));
		assertTrue("Should be an instance of Refreshable", messenger instanceof Refreshable);

		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	public void testScriptCompilationException() throws Exception {
		try {
			new ClassPathXmlApplicationContext("org/springframework/scripting/bsh/bshBrokenContext.xml");
			fail("Must throw exception for broken script file");
		}
		catch (NestedRuntimeException ex) {
			assertTrue(ex.contains(ScriptCompilationException.class));
		}
	}

	public void testScriptThatCompilesButIsJustPlainBad() throws Exception {
		ScriptSource script = mock(ScriptSource.class);
		final String badScript = "String getMessage() { throw new IllegalArgumentException(); }";
		given(script.getScriptAsString()).willReturn(badScript);
		given(script.isModified()).willReturn(true);
		BshScriptFactory factory = new BshScriptFactory(
				ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript,
				new Class<?>[] {Messenger.class});
		try {
			Messenger messenger = (Messenger) factory.getScriptedObject(script, new Class<?>[]{Messenger.class});
			messenger.getMessage();
			fail("Must have thrown a BshScriptUtils.BshExecutionException.");
		}
		catch (BshScriptUtils.BshExecutionException expected) {
		}
	}

	public void testCtorWithNullScriptSourceLocator() throws Exception {
		try {
			new BshScriptFactory(null, new Class<?>[] {Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptSourceLocator() throws Exception {
		try {
			new BshScriptFactory("", new Class<?>[] {Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithWhitespacedScriptSourceLocator() throws Exception {
		try {
			new BshScriptFactory("\n   ", new Class<?>[] {Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testResourceScriptFromTag() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		TestBean testBean = (TestBean) ctx.getBean("testBean");

		Collection<String> beanNames = Arrays.asList(ctx.getBeanNamesForType(Messenger.class));
		assertTrue(beanNames.contains("messenger"));
		assertTrue(beanNames.contains("messengerImpl"));
		assertTrue(beanNames.contains("messengerInstance"));

		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertEquals("Hello World!", messenger.getMessage());
		assertFalse(messenger instanceof Refreshable);

		Messenger messengerImpl = (Messenger) ctx.getBean("messengerImpl");
		assertEquals("Hello World!", messengerImpl.getMessage());

		Messenger messengerInstance = (Messenger) ctx.getBean("messengerInstance");
		assertEquals("Hello World!", messengerInstance.getMessage());

		TestBeanAwareMessenger messengerByType = (TestBeanAwareMessenger) ctx.getBean("messengerByType");
		assertEquals(testBean, messengerByType.getTestBean());

		TestBeanAwareMessenger messengerByName = (TestBeanAwareMessenger) ctx.getBean("messengerByName");
		assertEquals(testBean, messengerByName.getTestBean());

		Collection<Messenger> beans = ctx.getBeansOfType(Messenger.class).values();
		assertTrue(beans.contains(messenger));
		assertTrue(beans.contains(messengerImpl));
		assertTrue(beans.contains(messengerInstance));
		assertTrue(beans.contains(messengerByType));
		assertTrue(beans.contains(messengerByName));

		ctx.close();
		assertNull(messenger.getMessage());
		assertNull(messengerImpl.getMessage());
		assertNull(messengerInstance.getMessage());
	}

	public void testPrototypeScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertNotSame(messenger, messenger2);
		assertSame(messenger.getClass(), messenger2.getClass());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		assertEquals("Bye World!", messenger.getMessage());
		assertEquals("Byebye World!", messenger2.getMessage());
	}

	public void testInlineScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertNotNull(calculator);
		assertFalse(calculator instanceof Refreshable);
	}

	public void testRefreshableFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		assertEquals("Hello World!", messenger.getMessage());
		assertTrue("Messenger should be Refreshable", messenger instanceof Refreshable);
	}

	public void testApplicationEventListener() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("bsh-with-xsd.xml", getClass());
		Messenger eventListener = (Messenger) ctx.getBean("eventListener");
		ctx.publishEvent(new MyEvent(ctx));
		assertEquals("count=2", eventListener.getMessage());
	}


	@SuppressWarnings("serial")
	private static class MyEvent extends ApplicationEvent {

		public MyEvent(Object source) {
			super(source);
		}
	}

}
