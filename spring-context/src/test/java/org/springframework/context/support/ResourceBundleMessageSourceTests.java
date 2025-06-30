/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.support;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 03.02.2004
 */
class ResourceBundleMessageSourceTests {

	@Test
	void messageAccessWithDefaultMessageSource() {
		doTestMessageAccess(false, true, false, false, false);
	}

	@Test
	void messageAccessWithDefaultMessageSourceAndMessageFormat() {
		doTestMessageAccess(false, true, false, false, true);
	}

	@Test
	void messageAccessWithDefaultMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(false, true, true, true, false);
	}

	@Test
	void messageAccessWithDefaultMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(false, false, false, false, false);
	}

	@Test
	void messageAccessWithDefaultMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
		doTestMessageAccess(false, false, true, true, false);
	}

	@Test
	void messageAccessWithReloadableMessageSource() {
		doTestMessageAccess(true, true, false, false, false);
	}

	@Test
	void messageAccessWithReloadableMessageSourceAndMessageFormat() {
		doTestMessageAccess(true, true, false, false, true);
	}

	@Test
	void messageAccessWithReloadableMessageSourceAndFallbackToGerman() {
		doTestMessageAccess(true, true, true, true, false);
	}

	@Test
	void messageAccessWithReloadableMessageSourceAndFallbackTurnedOff() {
		doTestMessageAccess(true, false, false, false, false);
	}

	@Test
	void messageAccessWithReloadableMessageSourceAndFallbackTurnedOffAndFallbackToGerman() {
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
		Class<?> clazz = reloadable ? ReloadableResourceBundleMessageSource.class : ResourceBundleMessageSource.class;
		ac.registerSingleton("messageSource", clazz, pvs);
		ac.refresh();

		Locale.setDefault(expectGermanFallback ? Locale.GERMAN : Locale.CANADA);
		assertThat(ac.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		Object expected = (fallbackToSystemLocale && expectGermanFallback ? "nachricht2" : "message2");
		assertThat(ac.getMessage("code2", null, Locale.ENGLISH)).isEqualTo(expected);

		assertThat(ac.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		assertThat(ac.getMessage("code2", null, new Locale("DE", "at"))).isEqualTo("nochricht2");
		assertThat(ac.getMessage("code2", null, new Locale("DE", "at", "oo"))).isEqualTo("noochricht2");

		if (reloadable) {
			assertThat(ac.getMessage("code2", null, Locale.GERMANY)).isEqualTo("nachricht2xml");
		}

		MessageSourceAccessor accessor = new MessageSourceAccessor(ac);
		LocaleContextHolder.setLocale(new Locale("DE", "at"));
		try {
			assertThat(accessor.getMessage("code2")).isEqualTo("nochricht2");
		}
		finally {
			LocaleContextHolder.setLocale(null);
		}

		assertThat(ac.getMessage("code3", null, Locale.ENGLISH)).isEqualTo("message3");
		MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable("code3");
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");

		assertThat(ac.getMessage("code3", null, Locale.ENGLISH)).isEqualTo("message3");
		resolvable = new DefaultMessageSourceResolvable(new String[] {"code4", "code3"});
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("message3");

		Object[] args = new Object[] {"Hello", new DefaultMessageSourceResolvable(new String[] {"code1"})};
		assertThat(ac.getMessage("hello", args, Locale.ENGLISH)).isEqualTo("Hello, message1");

		// test default message without and with args
		assertThat(ac.getMessage(null, null, null, Locale.ENGLISH)).isNull();
		assertThat(ac.getMessage(null, null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage(null, args, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage(null, null, "{0}, default", Locale.ENGLISH)).isEqualTo("{0}, default");
		assertThat(ac.getMessage(null, args, "{0}, default", Locale.ENGLISH)).isEqualTo("Hello, default");

		// test resolvable with default message, without and with args
		resolvable = new DefaultMessageSourceResolvable(null, null, "default");
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("default");
		resolvable = new DefaultMessageSourceResolvable(null, args, "default");
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("default");
		resolvable = new DefaultMessageSourceResolvable(null, null, "{0}, default");
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("{0}, default");
		resolvable = new DefaultMessageSourceResolvable(null, args, "{0}, default");
		assertThat(ac.getMessage(resolvable, Locale.ENGLISH)).isEqualTo("Hello, default");

		// test message args
		assertThat(ac.getMessage("hello", new Object[]{"Arg1", "Arg2"}, Locale.ENGLISH)).isEqualTo("Arg1, Arg2");
		assertThat(ac.getMessage("hello", null, Locale.ENGLISH)).isEqualTo("{0}, {1}");

		if (alwaysUseMessageFormat) {
			assertThat(ac.getMessage("escaped", null, Locale.ENGLISH)).isEqualTo("I'm");
		}
		else {
			assertThat(ac.getMessage("escaped", null, Locale.ENGLISH)).isEqualTo("I''m");
		}
		assertThat(ac.getMessage("escaped", new Object[]{"some arg"}, Locale.ENGLISH)).isEqualTo("I'm");

		if (useCodeAsDefaultMessage) {
			assertThat(ac.getMessage("code4", null, Locale.GERMAN)).isEqualTo("code4");
		}
		else {
			assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
					ac.getMessage("code4", null, Locale.GERMAN));
		}
	}

	@Test
	@SuppressWarnings("resource")
	void defaultApplicationContextMessageSource() {
		GenericApplicationContext ac = new GenericApplicationContext();
		ac.refresh();
		assertThat(ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
		ac.close();
		assertThatIllegalStateException().isThrownBy(() -> ac.getMessage("code1", null, "default", Locale.ENGLISH));
	}

	@Test
	@SuppressWarnings("resource")
	void defaultApplicationContextMessageSourceWithParent() {
		GenericApplicationContext ac = new GenericApplicationContext();
		GenericApplicationContext parent = new GenericApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat(ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
		ac.close();
		assertThatIllegalStateException().isThrownBy(() -> ac.getMessage("code1", null, "default", Locale.ENGLISH));
	}

	@Test
	@SuppressWarnings("resource")
	void staticApplicationContextMessageSourceWithStaticParent() {
		StaticApplicationContext ac = new StaticApplicationContext();
		StaticApplicationContext parent = new StaticApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat(ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
		ac.close();
		assertThatIllegalStateException().isThrownBy(() -> ac.getMessage("code1", null, "default", Locale.ENGLISH));
	}

	@Test
	@SuppressWarnings("resource")
	void staticApplicationContextMessageSourceWithDefaultParent() {
		StaticApplicationContext ac = new StaticApplicationContext();
		GenericApplicationContext parent = new GenericApplicationContext();
		parent.refresh();
		ac.setParent(parent);
		ac.refresh();
		assertThat(ac.getMessage("code1", null, "default", Locale.ENGLISH)).isEqualTo("default");
		assertThat(ac.getMessage("code1", new Object[]{"value"}, "default {0}", Locale.ENGLISH)).isEqualTo("default value");
		ac.close();
		assertThatIllegalStateException().isThrownBy(() -> ac.getMessage("code1", null, "default", Locale.ENGLISH));
	}

	@Test
	void resourceBundleMessageSourceStandalone() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void resourceBundleMessageSourceWithWhitespaceInBasename() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void resourceBundleMessageSourceWithDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void resourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("argh");
		ms.setFallbackToSystemLocale(false);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	void reloadableResourceBundleMessageSourceStandalone() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithCacheSeconds() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheMillis(100);
		// Initial cache attempt
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		Thread.sleep(200);
		// Late enough for a re-cache attempt
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithNonConcurrentRefresh() throws InterruptedException {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setCacheMillis(100);
		ms.setConcurrentRefresh(false);
		// Initial cache attempt
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		Thread.sleep(200);
		// Late enough for a re-cache attempt
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithCommonMessages() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		Properties commonMessages = new Properties();
		commonMessages.setProperty("warning", "Do not do {0}");
		ms.setCommonMessages(commonMessages);
		ms.setBasename("org/springframework/context/support/messages");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
		assertThat(ms.getMessage("warning", new Object[]{"this"}, Locale.ENGLISH)).isEqualTo("Do not do this");
		assertThat(ms.getMessage("warning", new Object[]{"that"}, Locale.GERMAN)).isEqualTo("Do not do that");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithWhitespaceInBasename() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("  org/springframework/context/support/messages  ");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("ISO-8859-1");
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithInappropriateDefaultCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setDefaultEncoding("unicode");
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		ms.setFallbackToSystemLocale(false);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	void reloadableResourceBundleMessageSourceWithInappropriateEnglishCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages", "unicode");
		ms.setFileEncodings(fileCharsets);
		assertThatExceptionOfType(NoSuchMessageException.class).isThrownBy(() ->
				ms.getMessage("code1", null, Locale.ENGLISH));
	}

	@Test
	void reloadableResourceBundleMessageSourceWithInappropriateGermanCharset() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFallbackToSystemLocale(false);
		Properties fileCharsets = new Properties();
		fileCharsets.setProperty("org/springframework/context/support/messages_de", "unicode");
		ms.setFileEncodings(fileCharsets);
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("message2");
	}

	@Test
	void reloadableResourceBundleMessageSourceFileNameCalculation() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();

		List<String> filenames = ms.calculateFilenamesForLocale("messages", Locale.ENGLISH);
		assertThat(filenames).containsExactly("messages_en");

		filenames = ms.calculateFilenamesForLocale("messages", Locale.UK);
		assertThat(filenames).containsExactly("messages_en_GB", "messages_en");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "GB", "POSIX"));
		assertThat(filenames).containsExactly("messages_en_GB_POSIX","messages_en_GB", "messages_en");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("en", "", "POSIX"));
		assertThat(filenames).containsExactly("messages_en__POSIX", "messages_en");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "UK", "POSIX"));
		assertThat(filenames).containsExactly("messages__UK_POSIX", "messages__UK");

		filenames = ms.calculateFilenamesForLocale("messages", new Locale("", "", "POSIX"));
		assertThat(filenames).isEmpty();
	}

	@Test
	void reloadableResourceBundleMessageSourceWithCustomFileExtensions() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		ms.setFileExtensions(List.of(".toskip", ".custom"));
		assertThat(ms.getMessage("code1", null, Locale.ENGLISH)).isEqualTo("message1");
		assertThat(ms.getMessage("code2", null, Locale.GERMAN)).isEqualTo("nachricht2");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithEmptyCustomFileExtensions() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		assertThatThrownBy(() -> ms.setFileExtensions(Collections.emptyList()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("At least one file extension is required");
	}

	@Test
	void reloadableResourceBundleMessageSourceWithInvalidCustomFileExtensions() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		assertThatThrownBy(() -> ms.setFileExtensions(List.of("invalid")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("File extension 'invalid' should start with '.'");
	}

	@Test
	void messageSourceResourceBundle() {
		ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
		ms.setBasename("org/springframework/context/support/messages");
		MessageSourceResourceBundle rbe = new MessageSourceResourceBundle(ms, Locale.ENGLISH);
		assertThat(rbe.getString("code1")).isEqualTo("message1");
		assertThat(rbe.containsKey("code1")).isTrue();
		MessageSourceResourceBundle rbg = new MessageSourceResourceBundle(ms, Locale.GERMAN);
		assertThat(rbg.getString("code2")).isEqualTo("nachricht2");
		assertThat(rbg.containsKey("code2")).isTrue();
	}


	@AfterEach
	void tearDown() {
		ResourceBundle.clearCache();
	}

}
