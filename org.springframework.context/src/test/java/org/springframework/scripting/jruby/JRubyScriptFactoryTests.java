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

import java.util.Map;

import junit.framework.TestCase;
import junit.framework.Assert;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.ConfigurableMessenger;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.TestBeanAwareMessenger;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class JRubyScriptFactoryTests extends TestCase {

	private static final String RUBY_SCRIPT_SOURCE_LOCATOR =
			"inline:require 'java'\n" +
					"class RubyBar\n" +
					"end\n" +
					"RubyBar.new";


	public void testStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContext.xml", getClass());
		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		Assert.assertEquals(calc, calc);
		Assert.assertEquals(messenger, messenger);
		assertTrue(!messenger.equals(calc));
		assertNotSame(messenger.hashCode(), calc.hashCode());
		assertTrue(!messenger.toString().equals(calc.toString()));

		String desiredMessage = "Hello World!";
		Assert.assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
	}

	public void testNonStaticScript() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyRefreshableContext.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertTrue("Should be a proxy for refreshable scripts", AopUtils.isAopProxy(messenger));
		assertTrue("Should be an instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		Assert.assertEquals("Message is incorrect.", desiredMessage, messenger.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		Assert.assertEquals("Message is incorrect after refresh.", desiredMessage, messenger.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	public void testScriptCompilationException() throws Exception {
		try {
			new ClassPathXmlApplicationContext("jrubyBrokenContext.xml", getClass());
			fail("Should throw exception for broken script file");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(ScriptCompilationException.class));
		}
	}

	public void testCtorWithNullScriptSourceLocator() throws Exception {
		try {
			new JRubyScriptFactory(null, new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptSourceLocator() throws Exception {
		try {
			new JRubyScriptFactory("", new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithWhitespacedScriptSourceLocator() throws Exception {
		try {
			new JRubyScriptFactory("\n   ", new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithNullScriptInterfacesArray() throws Exception {
		try {
			new JRubyScriptFactory(RUBY_SCRIPT_SOURCE_LOCATOR, null);
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptInterfacesArray() throws Exception {
		try {
			new JRubyScriptFactory(RUBY_SCRIPT_SOURCE_LOCATOR, new Class[]{});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testResourceScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		TestBean testBean = (TestBean) ctx.getBean("testBean");

		Messenger messenger = (Messenger) ctx.getBean("messenger");
		Assert.assertEquals("Hello World!", messenger.getMessage());
		assertFalse(messenger instanceof Refreshable);

		TestBeanAwareMessenger messengerByType = (TestBeanAwareMessenger) ctx.getBean("messengerByType");
		Assert.assertEquals(testBean, messengerByType.getTestBean());

		TestBeanAwareMessenger messengerByName = (TestBeanAwareMessenger) ctx.getBean("messengerByName");
		Assert.assertEquals(testBean, messengerByName.getTestBean());
	}

	public void testPrototypeScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		ConfigurableMessenger messenger = (ConfigurableMessenger) ctx.getBean("messengerPrototype");
		ConfigurableMessenger messenger2 = (ConfigurableMessenger) ctx.getBean("messengerPrototype");

		assertNotSame(messenger, messenger2);
		assertSame(messenger.getClass(), messenger2.getClass());
		Assert.assertEquals("Hello World!", messenger.getMessage());
		Assert.assertEquals("Hello World!", messenger2.getMessage());
		messenger.setMessage("Bye World!");
		messenger2.setMessage("Byebye World!");
		Assert.assertEquals("Bye World!", messenger.getMessage());
		Assert.assertEquals("Byebye World!", messenger2.getMessage());
	}

	public void testInlineScriptFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertNotNull(calculator);
		assertFalse(calculator instanceof Refreshable);
	}

	public void testRefreshableFromTag() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		Assert.assertEquals("Hello World!", messenger.getMessage());
		assertTrue("Messenger should be Refreshable", messenger instanceof Refreshable);
	}

	public void testThatMultipleScriptInterfacesAreSupported() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("calculatingMessenger");
		Assert.assertEquals("Hello World!", messenger.getMessage());

		// cool, now check that the Calculator interface is also exposed
		Calculator calc = (Calculator) messenger;
		Assert.assertEquals(0, calc.add(2, -2));
	}

	public void testWithComplexArg() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContext.xml", getClass());
		Printer printer = (Printer) ctx.getBean("printer");
		CountingPrintable printable = new CountingPrintable();
		printer.print(printable);
		assertEquals(1, printable.count);
	}

	public void testWithPrimitiveArgsInReturnTypeAndParameters() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContextForPrimitives.xml", getClass());
		PrimitiveAdder adder = (PrimitiveAdder) ctx.getBean("adder");
		assertEquals(2, adder.addInts(1, 1));
		assertEquals(4, adder.addShorts((short) 1, (short) 3));
		assertEquals(5, adder.addLongs(2L, 3L));
		assertEquals(5, new Float(adder.addFloats(2.0F, 3.1F)).intValue());
		assertEquals(5, new Double(adder.addDoubles(2.0, 3.1)).intValue());
		assertFalse(adder.resultIsPositive(-200, 1));
		assertEquals("ri", adder.concatenate('r', 'i'));
		assertEquals('c', adder.echo('c'));
	}

	public void testWithWrapperArgsInReturnTypeAndParameters() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContextForWrappers.xml", getClass());
		WrapperAdder adder = (WrapperAdder) ctx.getBean("adder");

		assertEquals(new Integer(2), adder.addInts(new Integer(1), new Integer(1)));
		assertEquals(Integer.class, adder.addInts(new Integer(1), new Integer(1)).getClass());
		assertEquals(new Short((short) 4), adder.addShorts(new Short((short) 1), new Short((short) 3)));
		assertEquals(Short.class, adder.addShorts(new Short((short) 1), new Short((short) 3)).getClass());
		assertEquals(new Long(5L), adder.addLongs(new Long(2L), new Long(3L)));
		assertEquals(Long.class, adder.addLongs(new Long(2L), new Long(3L)).getClass());
		assertEquals(5, adder.addFloats(new Float(2.0F), new Float(3.1F)).intValue());
		assertEquals(Float.class, adder.addFloats(new Float(2.0F), new Float(3.1F)).getClass());
		assertEquals(5, new Double(adder.addDoubles(new Double(2.0), new Double(3.1)).intValue()).intValue());
		assertEquals(Double.class, adder.addDoubles(new Double(2.0), new Double(3.1)).getClass());
		assertFalse(adder.resultIsPositive(new Integer(-200), new Integer(1)).booleanValue());
		assertEquals(Boolean.class, adder.resultIsPositive(new Integer(-200), new Integer(1)).getClass());
		assertEquals("ri", adder.concatenate(new Character('r'), new Character('i')));
		assertEquals(String.class, adder.concatenate(new Character('r'), new Character('i')).getClass());
		assertEquals(new Character('c'), adder.echo(new Character('c')));
		assertEquals(Character.class, adder.echo(new Character('c')).getClass());
		Integer[] numbers = new Integer[]{new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)};
		assertEquals("12345", adder.concatArrayOfIntegerWrappers(numbers));
		assertEquals(String.class, adder.concatArrayOfIntegerWrappers(numbers).getClass());

		Short[] shorts = adder.populate(new Short((short) 1), new Short((short) 2));
		assertEquals(2, shorts.length);
		assertNotNull(shorts[0]);
		assertEquals(new Short((short) 1), shorts[0]);
		assertNotNull(shorts[1]);
		assertEquals(new Short((short) 2), shorts[1]);

		String[][] lol = adder.createListOfLists("1", "2", "3");
		assertNotNull(lol);
		assertEquals(3, lol.length);
		assertEquals("1", lol[0][0]);
		assertEquals("2", lol[1][0]);
		assertEquals("3", lol[2][0]);

		Map singleValueMap = adder.toMap("key", "value");
		assertNotNull(singleValueMap);
		assertEquals(1, singleValueMap.size());
		assertEquals("key", singleValueMap.keySet().iterator().next());
		assertEquals("value", singleValueMap.values().iterator().next());

		String[] expectedStrings = new String[]{"1", "2", "3"};
		Map map = adder.toMap("key", expectedStrings);
		assertNotNull(map);
		assertEquals(1, map.size());
		assertEquals("key", map.keySet().iterator().next());
		String[] strings = (String[]) map.values().iterator().next();
		for (int i = 0; i < expectedStrings.length; ++i) {
			assertEquals(expectedStrings[i], strings[i]);
		}
	}

	public void testAOP() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-aop.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		Assert.assertEquals(new StringBuffer("Hello World!").reverse().toString(), messenger.getMessage());
	}


	private static final class CountingPrintable implements Printable {

		public int count;

		public String getContent() {
			this.count++;
			return "Hello World!";
		}
	}

}
