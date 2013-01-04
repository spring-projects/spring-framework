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

package org.springframework.context.support;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.context.ACATester;
import org.springframework.context.AbstractApplicationContextTests;
import org.springframework.context.BeanThatListens;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class StaticMessageSourceTests extends AbstractApplicationContextTests {

	protected static final String MSG_TXT1_US =
			"At '{1,time}' on \"{1,date}\", there was \"{2}\" on planet {0,number,integer}.";
	protected static final String MSG_TXT1_UK =
			"At '{1,time}' on \"{1,date}\", there was \"{2}\" on station number {0,number,integer}.";
	protected static final String MSG_TXT2_US =
			"This is a test message in the message catalog with no args.";
	protected static final String MSG_TXT3_US =
			"This is another test message in the message catalog with no args.";

	protected StaticApplicationContext sac;

	/** Overridden */
	@Override
	public void testCount() {
		// These are only checked for current Ctx (not parent ctx)
		assertCount(15);
	}

	@Override
	public void testMessageSource() throws NoSuchMessageException {
		// Do nothing here since super is looking for errorCodes we
		// do NOT have in the Context
	}

	public void testGetMessageWithDefaultPassedInAndFoundInMsgCatalog() {
		// Try with Locale.US
		assertTrue("valid msg from staticMsgSource with default msg passed in returned msg from msg catalog for Locale.US",
				sac.getMessage("message.format.example2", null, "This is a default msg if not found in MessageSource.", Locale.US)
				.equals("This is a test message in the message catalog with no args."));
	}

	public void testGetMessageWithDefaultPassedInAndNotFoundInMsgCatalog() {
		// Try with Locale.US
		assertTrue("bogus msg from staticMsgSource with default msg passed in returned default msg for Locale.US",
				sac.getMessage("bogus.message", null, "This is a default msg if not found in MessageSource.", Locale.US)
				.equals("This is a default msg if not found in MessageSource."));
	}

	/**
	 * We really are testing the AbstractMessageSource class here.
	 * The underlying implementation uses a hashMap to cache messageFormats
	 * once a message has been asked for.  This test is an attempt to
	 * make sure the cache is being used properly.
	 * @see org.springframework.context.support.AbstractMessageSource for more details.
	 */
	public void testGetMessageWithMessageAlreadyLookedFor() {
		Object[] arguments = {
			new Integer(7), new Date(System.currentTimeMillis()),
			"a disturbance in the Force"
		};

		// The first time searching, we don't care about for this test
		// Try with Locale.US
		sac.getMessage("message.format.example1", arguments, Locale.US);

		// Now msg better be as expected
		assertTrue("2nd search within MsgFormat cache returned expected message for Locale.US",
				sac.getMessage("message.format.example1", arguments, Locale.US).indexOf(
						"there was \"a disturbance in the Force\" on planet 7.") != -1);

		Object[] newArguments = {
			new Integer(8), new Date(System.currentTimeMillis()),
			"a disturbance in the Force"
		};

		// Now msg better be as expected even with different args
		assertTrue("2nd search within MsgFormat cache with different args returned expected message for Locale.US",
				sac.getMessage("message.format.example1", newArguments, Locale.US)
				.indexOf("there was \"a disturbance in the Force\" on planet 8.") != -1);
	}

	/**
	 * Example taken from the javadocs for the java.text.MessageFormat class
	 */
	public void testGetMessageWithNoDefaultPassedInAndFoundInMsgCatalog() {
		Object[] arguments = {
			new Integer(7), new Date(System.currentTimeMillis()),
			"a disturbance in the Force"
		};

		/*
		 Try with Locale.US
		 Since the msg has a time value in it, we will use String.indexOf(...)
		 to just look for a substring without the time.  This is because it is
		 possible that by the time we store a time variable in this method
		 and the time the ResourceBundleMessageSource resolves the msg the
		 minutes of the time might not be the same.
		 */
		assertTrue("msg from staticMsgSource for Locale.US substituting args for placeholders is as expected",
				sac.getMessage("message.format.example1", arguments, Locale.US)
				.indexOf("there was \"a disturbance in the Force\" on planet 7.") != -1);

		// Try with Locale.UK
		assertTrue("msg from staticMsgSource for Locale.UK substituting args for placeholders is as expected",
				sac.getMessage("message.format.example1", arguments, Locale.UK)
				.indexOf("there was \"a disturbance in the Force\" on station number 7.") != -1);

		// Try with Locale.US - Use a different test msg that requires no args
		assertTrue("msg from staticMsgSource for Locale.US that requires no args is as expected",
				sac.getMessage("message.format.example2", null, Locale.US)
				.equals("This is a test message in the message catalog with no args."));
	}

	public void testGetMessageWithNoDefaultPassedInAndNotFoundInMsgCatalog() {
		// Expecting an exception
		try {
			// Try with Locale.US
			sac.getMessage("bogus.message", null, Locale.US);

			fail("bogus msg from staticMsgSource for Locale.US without default msg should have thrown exception");
		}
		catch (NoSuchMessageException tExcept) {
			assertTrue("bogus msg from staticMsgSource for Locale.US without default msg threw expected exception", true);
		}
	}

	public void testMessageSourceResolvable() {
		// first code valid
		String[] codes1 = new String[] {"message.format.example3", "message.format.example2"};
		MessageSourceResolvable resolvable1 = new DefaultMessageSourceResolvable(codes1, null, "default");
		try {
			assertTrue("correct message retrieved", MSG_TXT3_US.equals(sac.getMessage(resolvable1, Locale.US)));
		}
		catch (NoSuchMessageException ex) {
			fail("Should not throw NoSuchMessageException");
		}

		// only second code valid
		String[] codes2 = new String[] {"message.format.example99", "message.format.example2"};
		MessageSourceResolvable resolvable2 = new DefaultMessageSourceResolvable(codes2, null, "default");
		try {
			assertTrue("correct message retrieved", MSG_TXT2_US.equals(sac.getMessage(resolvable2, Locale.US)));
		}
		catch (NoSuchMessageException ex) {
			fail("Should not throw NoSuchMessageException");
		}

		// no code valid, but default given
		String[] codes3 = new String[] {"message.format.example99", "message.format.example98"};
		MessageSourceResolvable resolvable3 = new DefaultMessageSourceResolvable(codes3, null, "default");
		try {
			assertTrue("correct message retrieved", "default".equals(sac.getMessage(resolvable3, Locale.US)));
		}
		catch (NoSuchMessageException ex) {
			fail("Should not throw NoSuchMessageException");
		}

		// no code valid, no default
		String[] codes4 = new String[] {"message.format.example99", "message.format.example98"};
		MessageSourceResolvable resolvable4 = new DefaultMessageSourceResolvable(codes4);
		try {
			sac.getMessage(resolvable4, Locale.US);
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected
		}
	}

	/** Run for each test */
	@Override
	protected ConfigurableApplicationContext createContext() throws Exception {
		StaticApplicationContext parent = new StaticApplicationContext();

		Map<String, String> m = new HashMap<String, String>();
		m.put("name", "Roderick");
		parent.registerPrototype("rod", org.springframework.tests.sample.beans.TestBean.class, new MutablePropertyValues(m));
		m.put("name", "Albert");
		parent.registerPrototype("father", org.springframework.tests.sample.beans.TestBean.class, new MutablePropertyValues(m));

		parent.refresh();
		parent.addApplicationListener(parentListener);

		this.sac = new StaticApplicationContext(parent);

		sac.registerSingleton("beanThatListens", BeanThatListens.class, new MutablePropertyValues());

		sac.registerSingleton("aca", ACATester.class, new MutablePropertyValues());

		sac.registerPrototype("aca-prototype", ACATester.class, new MutablePropertyValues());

		PropertiesBeanDefinitionReader reader = new PropertiesBeanDefinitionReader(sac.getDefaultListableBeanFactory());
		reader.loadBeanDefinitions(new ClassPathResource("testBeans.properties", getClass()));
		sac.refresh();
		sac.addApplicationListener(listener);

		StaticMessageSource messageSource = sac.getStaticMessageSource();
		Map<String, String> usMessages = new HashMap<String, String>(3);
		usMessages.put("message.format.example1", MSG_TXT1_US);
		usMessages.put("message.format.example2", MSG_TXT2_US);
		usMessages.put("message.format.example3", MSG_TXT3_US);
		messageSource.addMessages(usMessages, Locale.US);
		messageSource.addMessage("message.format.example1", Locale.UK, MSG_TXT1_UK);

		return sac;
	}

	public void testNestedMessageSourceWithParamInChild() {
		StaticMessageSource source = new StaticMessageSource();
		StaticMessageSource parent = new StaticMessageSource();
		source.setParentMessageSource(parent);

		source.addMessage("param", Locale.ENGLISH, "value");
		parent.addMessage("with.param", Locale.ENGLISH, "put {0} here");

		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
				new String[] {"with.param"}, new Object[] {new DefaultMessageSourceResolvable("param")});

		assertEquals("put value here", source.getMessage(resolvable, Locale.ENGLISH));
	}

	public void testNestedMessageSourceWithParamInParent() {
		StaticMessageSource source = new StaticMessageSource();
		StaticMessageSource parent = new StaticMessageSource();
		source.setParentMessageSource(parent);

		parent.addMessage("param", Locale.ENGLISH, "value");
		source.addMessage("with.param", Locale.ENGLISH, "put {0} here");

		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
				new String[] {"with.param"}, new Object[] {new DefaultMessageSourceResolvable("param")});

		assertEquals("put value here", source.getMessage(resolvable, Locale.ENGLISH));
	}

}
