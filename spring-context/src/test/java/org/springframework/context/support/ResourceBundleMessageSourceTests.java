/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 03.02.2004
 */
public class ResourceBundleMessageSourceTests {

	@Test
	public void testMessageAccessWithDefaultMessageSource() {
		doTestMessageAccess(false, true, false, false, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndMessageFormat() {
		doTestMessageAccess(false, true, false, false, true);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(false, true, true, true, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(false, false, false, false, false);
	}

	@Test
	public void testMessageAccessWithDefaultMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
		doTestMessageAccess(false, false, true, true, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSource() {
		doTestMessageAccess(true, true, false, false, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndMessageFormat() {
		doTestMessageAccess(true, true, false, false, true);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(true, true, true, true, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(true, false, false, false, false);
	}

	@Test
	public void testMessageAccessWithReloadableMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
		doTestMessageAccess(true, false, true, true, false);
	}

	protected void doTestMessageAccess(
			boolean reloadable, boolean fallbackToSystemLocale,
			boolean expectGermanFallback, boolean useCodeAsDefaultMessage, boolean alwaysUseMessageFormat) {

		StaticApplicationContext ac = new StaticApplicationContext();
		if (reloadable) {
			StaticApplicationContext parent = new StaticApplicationContext();
			parent.refresh();
			ac.setParent(parent);
		}

		MutablePropertyValues pvs = new MutablePropertyValues();
		String basepath = "org/springframework/context/support/";
		String[] basenames;
		if (reloadable) {
			basenames = new String[] {
					"classpath:" + basepath + "messages",
					"classpath:" + basepath + "more-messages"};
		}
		else {
			basenames = new String[] {
					basepath + "messages",
					basepath + "more-messages"};
		}
		pvs.add("basenames", basenames);
		if (!fallbackToSystemLocale) {
			pvs.add("fallbackToSystemLocale", Boolean.FALSE);
		}
		if (useCodeAsDefaultMessage) {
			pvs.add("useCodeAsDefaultMessage", Boolean.TRUE);
		}
		if (alwaysUseMessageFormat) {
			pvs.add("alwaysUseMessageFormat", Boolean.TRUE);
		}
		Class<?> clazz = reloadable ?
				ReloadableResourceBundleMessageSource.class : ResourceBundleMessageSource.class;
		ac.registerSingleton("messageSource", clazz, pvs);
		ac.refresh();

		Locale.setDefault(expectGermanFallback ? Locale.GERMAN : Locale.CANADA);
		assertEquals("message1", ac.getMessage("code1", null, Locale.ENGLISH));
		assertEquals(fallbackToSystemLocale && expectGermanFallback ? "nachricht2" : "message2",
				ac.getMessage("code2", null, Locale.ENGLISH));

		assertEquals("nachricht2", ac.getMessage("code2", null, Locale.GERMAN));
		assertEquals("nochricht2", ac.getMessage("code2", null, new Locale("DE", "at")));
		assertEquals("noochricht2", ac.getMessage("code2", null, new Locale("DE", "at", "oo")));

		if (reloadable) {
			assertEquals("nachricht2xml", ac.getMessage("code2", null, Locale.GERMANY));
		}

		MessageSourceAccessor accessor = new MessageSourceAccessor(ac);
		LocaleContextHolder.setLocale(new Locale("DE", "at"));
		try {
			assertEquals("nochricht2", accessor.getMessage("code2"));
		}
		finally {
			LocaleContextHolder.setLocale(null);
		}

		assertEquals("message3", ac.getMessage("code3", null, Locale.ENGLISH));
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable("code3");
		assertEquals("message3", ac.getMessage(resolvable, Locale.ENGLISH));
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertEquals("message3", ac.getMessage(resolvable, Locale.ENGLISH));

		assertEquals("message3", ac.getMessage("code3", null, Locale.ENGLISH));
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertEquals("message3", ac.getMessage(resolvable, Locale.ENGLISH));

		Object[] args = new Object[] {"Hello", new DefaultMessageSourceResolvable(new String[] {"code1"})};
		assertEquals("Hello, message1", ac.getMessage("hello", args, Locale.ENGLISH));

		// test default message without and with args
		assertEquals("default", ac.getMessage(null, null, "default", Locale.ENGLISH));
		assertEquals("default", ac.getMessage(null, args, "default", Locale.ENGLISH));
		assertEquals("{0}, default", ac.getMessage(null, null, "{0}, default", Locale.ENGLISH));
		assertEquals("Hello, default", ac.getMessage(null, args, "{0}, default", Locale.ENGLISH));

		// test resolvable with default message, without and with args
		resolvable = new DefaultMessageSourceResolvable(null, null, "default");
		assertEquals("default", ac.getMessage(resolvable, Locale.ENGLISH));
		resolvable = new DefaultMessageSourceResolvable(null, args, "default");
		assertEquals("default", ac.getMessage(resolvable, Locale.ENGLISH));
		resolvable = new DefaultMessageSourceResolvable(null, null, "{0}, default");
		assertEquals("{0}, default", ac.getMessage(resolvable, Locale.ENGLISH));
		resolvable = new DefaultMessageSourceResolvable(null, args, "{0}, default");
		assertEquals("Hello, default", ac.getMessage(resolvable, Locale.ENGLISH));

		// test message args
		assertEquals("Arg1, Arg2", ac.getMessage("hello", new Object[] {"Arg1", "Arg2"}, Locale.ENGLISH));
		assertEquals("{0}, {1}", ac.getMessage("hello", null, Locale.ENGLISH));

		if (alwaysUseMessageFormat) {
			assertEquals("I'm", ac.getMessage("escaped", null, Locale.ENGLISH));
		}
		else {
			assertEquals("I''m", ac.getMessage("escaped", null, Locale.ENGLISH));
		}
		assertEquals("I'm", ac.getMessage("escaped", new Object[] {"some arg"}, Locale.ENGLISH));

		try {
			assertEquals("code4", ac.getMessage("code4", null, Locale.GERMAN));
			if (!useCodeAsDefaultMessage) {
				fail("Should have thrown NoSuchMessageException");
			}
		}
		catch (NoSuchMessageException ex) {
			if (useCodeAsDefaultMessage) {
				fail("Should have returned code as default message");
			}
		}
	}

	@Test
	public void testDefaultApplicationContextMessageSource() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.refresh();
		assertEquals("default", ac.getMessage("code1", null, "default", Locale.ENGLISH));
		assertEquals("default value", ac.getMessage("code1", new Object[] {"value"}, "default {0}", Locale.ENGLISH));
	}

	@Test
	public void testResourceBundleMessageSourceStandalone() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testResourceBundleMessageSourceWithWhitespaceInBasename() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testResourceBundleMessageSourceWithDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testResourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("argh");
		ms.setFallbackToSystemLocale(false);
		try {
			ms.getMessage("code1", null, Locale.ENGLISH);
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected
		}
	}

	@Test
	public void testReloadableResourceBundleMessageSourceStandalone() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithCacheSeconds() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheSeconds(1);
		// Initial cache attempt
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
		Thread.sleep(1100);
		// Late enough for a re-cache attempt
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithNonConcurrentRefresh() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheSeconds(1);
		ms.setConcurrentRefresh(false);
		// Initial cache attempt
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
		Thread.sleep(1100);
		// Late enough for a re-cache attempt
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithCommonMessages() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		Properties commonMessages = new Properties();
		commonMessages.setProperty("warning", "Do not do {0}");
		ms.setCommonMessages(commonMessages);
		ms.setBasename("org/springframework/context/support/messages");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
		assertEquals("Do not do this", ms.getMessage("warning", new Object[] {"this"}, Locale.ENGLISH));
		assertEquals("Do not do that", ms.getMessage("warning", new Object[] {"that"}, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithWhitespaceInBasename() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("nachricht2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("unicode");
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		ms.setFallbackToSystemLocale(false);
		try {
			ms.getMessage("code1", null, Locale.ENGLISH);
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected
		}
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateEnglishCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages", "unicode");
		ms.setFileEncodings(fileCharsets);
		try {
			ms.getMessage("code1", null, Locale.ENGLISH);
			fail("Should have thrown NoSuchMessageException");
		}
		catch (NoSuchMessageException ex) {
			// expected
		}
	}

	@Test
	public void testReloadableResourceBundleMessageSourceWithInappropriateGermanCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		assertEquals("message1", ms.getMessage("code1", null, Locale.ENGLISH));
		assertEquals("message2", ms.getMessage("code2", null, Locale.GERMAN));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceFileNameDotSeparatorDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org.springframework.context.support.more-messages");
		assertEquals("message3", ms.getMessage("code3", null, Locale.ENGLISH));
	}

	@Test
	public void testReloadableResourceBundleMessageSourceFileNameCalculation() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();

		List<String> filenames = ms.calculateFilenamesForLocale("messages", Locale.ENGLISH);
		assertEquals(1, filenames.size());
		assertEquals("messages_en", filenames.get(0));

		filenames = ms.calculateFilenamesForLocale("messages", Locale.UK);
		assertEquals(2, filenames.size());
		assertEquals("messages_en", filenames.get(1));
		assertEquals("messages_en_GB", filenames.get(0));

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "GB", "POSIX"));
		assertEquals(3, filenames.size());
		assertEquals("messages_en", filenames.get(2));
		assertEquals("messages_en_GB", filenames.get(1));
		assertEquals("messages_en_GB_POSIX", filenames.get(0));

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "", "POSIX"));
		assertEquals(2, filenames.size());
		assertEquals("messages_en", filenames.get(1));
		assertEquals("messages_en__POSIX", filenames.get(0));

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "UK", "POSIX"));
		assertEquals(2, filenames.size());
		assertEquals("messages__UK", filenames.get(1));
		assertEquals("messages__UK_POSIX", filenames.get(0));

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "", "POSIX"));
		assertEquals(0, filenames.size());
	}

	@Test
	public void testMessageSourceResourceBundle() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		MessageSourceResourceBundle rbe = new MessageSourceResourceBundle(ms, Locale.ENGLISH);
		assertEquals("message1", rbe.getString("code1"));
		assertTrue(rbe.containsKey("code1"));
		MessageSourceResourceBundle rbg = new MessageSourceResourceBundle(ms, Locale.GERMAN);
		assertEquals("nachricht2", rbg.getString("code2"));
		assertTrue(rbg.containsKey("code2"));
	}


	@After
	public void tearDown() {
		ResourceBundle.clearCache();
	}

}
