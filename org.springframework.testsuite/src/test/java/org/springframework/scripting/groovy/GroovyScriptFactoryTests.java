/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.scripting.groovy;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;

import groovy.lang.DelegatingMetaClass;
import groovy.lang.GroovyObject;
import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.NestedRuntimeException;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.CallCounter;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.ContextScriptBean;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ScriptFactoryPostProcessor;
import org.springframework.test.AssertThrows;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
public class GroovyScriptFactoryTests extends TestCase {

	public void testStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());

		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Calculator.class)).contains("calculator"));
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messenger"));

		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(calc));
		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(messenger));

		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertEquals(calc, calc);
		assertEquals(messenger, messenger);
		assertTrue(!messenger.equals(calc));
		assertTrue(messenger.hashCode() != calc.hashCode());
		assertTrue(!messenger.toString().equals(calc.toString()));

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		assertTrue(ctx.getBeansOfType(Calculator.class).values().contains(calc));
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testStaticPrototypeScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
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

	public void testStaticScriptWithInstance() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstance"));
		Messenger messenger = (Messenger) ctx.getBean("messengerInstance");

		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(messenger));
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testStaticScriptWithInlineDefinedInstance() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("messengerInstanceInline"));
		Messenger messenger = (Messenger) ctx.getBean("messengerInstanceInline");

		assertFalse("Shouldn't get proxy when refresh is disabled", AopUtils.isAopProxy(messenger));
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testNonStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertTrue("Should be a proxy for refreshable scripts", AopUtils.isAopProxy(messenger));
		assertTrue("Should be an instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Message is incorrect after refresh.", desiredMessage, messenger.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	public void testNonStaticPrototypeScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovyRefreshableContext.xml", getClass());
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
			new ClassPathXmlApplicationContext("org/springframework/scripting/groovy/groovyBrokenContext.xml");
			fail("Should throw exception for broken script file");
		}
		catch (NestedRuntimeException ex) {
			assertTrue("Wrong root cause: " + ex, ex.contains(ScriptCompilationException.class));
		}
	}

	public void testScriptedClassThatDoesNotHaveANoArgCtor() throws Exception {
		MockControl mock = MockControl.createControl(ScriptSource.class);
		ScriptSource script = (ScriptSource) mock.getMock();
		script.getScriptAsString();
		final String badScript = "class Foo { public Foo(String foo) {}}";
		mock.setReturnValue(badScript);
		script.suggestedClassName();
		mock.setReturnValue("someName");
		mock.replay();
		GroovyScriptFactory factory = new GroovyScriptFactory(ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript);
		try {
			factory.getScriptedObject(script, new Class[]{});
			fail("Must have thrown a ScriptCompilationException (no public no-arg ctor in scripted class).");
		}
		catch (ScriptCompilationException expected) {
			assertTrue(expected.contains(InstantiationException.class));
		}
		mock.verify();
	}

	public void testScriptedClassThatHasNoPublicNoArgCtor() throws Exception {
		MockControl mock = MockControl.createControl(ScriptSource.class);
		ScriptSource script = (ScriptSource) mock.getMock();
		script.getScriptAsString();
		final String badScript = "class Foo { protected Foo() {}}";
		mock.setReturnValue(badScript);
		script.suggestedClassName();
		mock.setReturnValue("someName");
		mock.replay();
		GroovyScriptFactory factory = new GroovyScriptFactory(ScriptFactoryPostProcessor.INLINE_SCRIPT_PREFIX + badScript);
		try {
			factory.getScriptedObject(script, new Class[]{});
			fail("Must have thrown a ScriptCompilationException (no oublic no-arg ctor in scripted class).");
		}
		catch (ScriptCompilationException expected) {
			assertTrue(expected.contains(IllegalAccessException.class));
		}
		mock.verify();
	}

	public void testWithTwoClassesDefinedInTheOneGroovyFile_CorrectClassFirst() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("twoClassesCorrectOneFirst.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertNotNull(messenger);
		assertEquals("Hello World!", messenger.getMessage());

		// Check can cast to GroovyObject
		GroovyObject goo = (GroovyObject) messenger;
	}

	public void testWithTwoClassesDefinedInTheOneGroovyFile_WrongClassFirst() throws Exception {
		try {
			ApplicationContext ctx = new ClassPathXmlApplicationContext("twoClassesWrongOneFirst.xml", getClass());
			ctx.getBean("messenger", Messenger.class);
			fail("Must have failed: two classes defined in GroovyScriptFactory source, non-Messenger class defined first.");
		}
		// just testing for failure here, hence catching Exception...
		catch (Exception expected) {
		}
	}

	public void testCtorWithNullScriptSourceLocator() throws Exception {
		try {
			new GroovyScriptFactory(null);
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptSourceLocator() throws Exception {
		try {
			new GroovyScriptFactory("");
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithWhitespacedScriptSourceLocator() throws Exception {
		try {
			new GroovyScriptFactory("\n   ");
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testWithInlineScriptWithLeadingWhitespace() throws Exception {
		try {
			new ClassPathXmlApplicationContext("lwspBadGroovyContext.xml", getClass());
			fail("Must have thrown a BeanCreationException ('inline:' prefix was preceded by whitespace");
		}
		catch (BeanCreationException expected) {
			assertTrue(expected.contains(FileNotFoundException.class));
		}
	}

	public void testGetScriptedObjectDoesNotChokeOnNullInterfacesBeingPassedIn() throws Exception {
		MockControl mock = MockControl.createControl(ScriptSource.class);
		ScriptSource scriptSource = (ScriptSource) mock.getMock();
		scriptSource.getScriptAsString();
		mock.setReturnValue("class Bar {}");
		scriptSource.suggestedClassName();
		mock.setReturnValue("someName");
		mock.replay();

		GroovyScriptFactory factory = new GroovyScriptFactory("a script source locator (doesn't matter here)");
		Object scriptedObject = factory.getScriptedObject(scriptSource, null);
		assertNotNull(scriptedObject);
		mock.verify();
	}

	public void testGetScriptedObjectDoesChokeOnNullScriptSourceBeingPassedIn() throws Exception {
		GroovyScriptFactory factory = new GroovyScriptFactory("a script source locator (doesn't matter here)");
		try {
			factory.getScriptedObject(null, null);
			fail("Must have thrown a NullPointerException as per contract ('null' ScriptSource supplied");
		}
		catch (NullPointerException expected) {
		}
	}

	public void testResourceScriptFromTag() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		CallCounter countingAspect = (CallCounter) ctx.getBean("getMessageAspect");

		assertTrue(AopUtils.isAopProxy(messenger));
		assertFalse(messenger instanceof Refreshable);
		assertEquals(0, countingAspect.getCalls());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals(1, countingAspect.getCalls());

		ctx.close();
		assertEquals(-200, countingAspect.getCalls());
	}

	public void testPrototypeScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
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
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertNotNull(calculator);
		assertFalse(calculator instanceof Refreshable);
	}

	public void testRefreshableFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		assertTrue(Arrays.asList(ctx.getBeanNamesForType(Messenger.class)).contains("refreshableMessenger"));

		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		CallCounter countingAspect = (CallCounter) ctx.getBean("getMessageAspect");

		assertTrue(AopUtils.isAopProxy(messenger));
		assertTrue(messenger instanceof Refreshable);
		assertEquals(0, countingAspect.getCalls());
		assertEquals("Hello World!", messenger.getMessage());
		assertEquals(1, countingAspect.getCalls());

		assertTrue(ctx.getBeansOfType(Messenger.class).values().contains(messenger));
	}

	public void testAnonymousScriptDetected() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-with-xsd.xml", getClass());
		Map beans = ctx.getBeansOfType(Messenger.class);
		assertEquals(4, beans.size());
	}

	/**
	 * Tests the SPR-2098 bug whereby no more than 1 property element could be
	 * passed to a scripted bean :(
	 */
	public void testCanPassInMoreThanOneProperty() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("groovy-multiple-properties.xml", getClass());
		TestBean tb = (TestBean) ctx.getBean("testBean");

		ContextScriptBean bean = (ContextScriptBean) ctx.getBean("bean");
		assertEquals("The first property ain't bein' injected.", "Sophie Marceau", bean.getName());
		assertEquals("The second property ain't bein' injected.", 31, bean.getAge());
		assertEquals(tb, bean.getTestBean());
		assertEquals(ctx, bean.getApplicationContext());

		ContextScriptBean bean2 = (ContextScriptBean) ctx.getBean("bean2");
		assertEquals(tb, bean2.getTestBean());
		assertEquals(ctx, bean2.getApplicationContext());

		try {
			ctx.getBean("bean3");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
			assertTrue(ex.contains(UnsatisfiedDependencyException.class));
		}
	}

	public void testMetaClassWithBeans() {
		testMetaClass("org/springframework/scripting/groovy/calculators.xml");
	}
	
	public void testMetaClassWithXsd() {
		testMetaClass("org/springframework/scripting/groovy/calculators-with-xsd.xml");
	}
	
	private void testMetaClass(final String xmlFile) {
		// expect the exception we threw in the custom metaclass to show it got invoked
		AssertThrows at = new AssertThrows(IllegalStateException.class) {
			public void test() throws Exception {
				ApplicationContext ctx =
						new ClassPathXmlApplicationContext(xmlFile);
				Calculator calc = (Calculator) ctx.getBean("delegatingCalculator");
				calc.add(1, 2);
			}
		};
		at.runTest();
		assertEquals("Gotcha", at.getActualException().getMessage());
	}

	public void testFactoryBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		Object factory = context.getBean("&factory");
		assertTrue(factory instanceof FactoryBean);
		Object result = context.getBean("factory");
		assertTrue(result instanceof String);
		assertEquals("test", result);
	}

	public void testRefreshableFactoryBean() {
		ApplicationContext context = new ClassPathXmlApplicationContext("groovyContext.xml", getClass());
		Object factory = context.getBean("&refreshableFactory");
		assertTrue(factory instanceof FactoryBean);
		Object result = context.getBean("refreshableFactory");
		assertTrue(result instanceof String);
		assertEquals("test", result);
	}


	public static class TestCustomizer implements GroovyObjectCustomizer {

		public void customize(GroovyObject goo) {
			DelegatingMetaClass dmc = new DelegatingMetaClass(goo.getMetaClass()) {
				public Object invokeMethod(Object arg0, String mName, Object[] arg2) {
					if (mName.indexOf("Missing") != -1) {
						throw new IllegalStateException("Gotcha");
					}
					else {
						return super.invokeMethod(arg0, mName, arg2);
					}
				}
			};
			dmc.initialize();
			goo.setMetaClass(dmc);
		}
	}

}
