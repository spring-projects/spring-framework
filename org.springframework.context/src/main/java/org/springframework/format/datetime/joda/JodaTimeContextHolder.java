/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.core.NamedInheritableThreadLocal;

/**
 * A holder for a thread-local user {@link JodaTimeContext}.
 *
 * @since 3.0
 * @author Keith Donald
 */
public final class JodaTimeContextHolder {

	private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder =
			new NamedInheritableThreadLocal<JodaTimeContext>("JodaTime Context");


	/**
	 * Associate the given JodaTimeContext with the current thread.
	 * @param context the current JodaTimeContext, or <code>null</code> to clear
	 * the thread-bound context
	 */
	public static void setJodaTimeContext(JodaTimeContext context) {
		jodaTimeContextHolder.set(context);
	}

	/**
	 * Return the JodaTimeContext associated with the current thread, if any.
	 * @return the current JodaTimeContext, or <code>null</code> if none
	 */
	public static JodaTimeContext getJodaTimeContext() {
		return jodaTimeContextHolder.get();
	}

	/**
	 * Gets the Formatter with the user-specific settings applied to thefrom the base <code>formatter</code>.
	 * @param formatter the base formatter that establishes default formatting rules, generally user independent
	 * @param locale the current user locale (may be null if not known)
	 * @return the user's DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
		if (locale != null) {
			formatter = formatter.withLocale(locale);
		}
		JodaTimeContext context = getJodaTimeContext();
		return (context != null ? context.getFormatter(formatter) : formatter);
	}
	
}
