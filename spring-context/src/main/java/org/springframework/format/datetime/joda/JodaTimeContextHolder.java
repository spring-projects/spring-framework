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

package org.springframework.format.datetime.joda;

import java.util.Locale;

import org.joda.time.format.DateTimeFormatter;

import org.springframework.core.NamedThreadLocal;

/**
 * A holder for a thread-local user {@link JodaTimeContext}.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public final class JodaTimeContextHolder {

	private static final ThreadLocal<JodaTimeContext> jodaTimeContextHolder =
			new NamedThreadLocal<JodaTimeContext>("JodaTime Context");


	/**
	 * Reset the JodaTimeContext for the current thread.
	 */
	public static void resetJodaTimeContext() {
		jodaTimeContextHolder.remove();
	}

	/**
	 * Associate the given JodaTimeContext with the current thread.
	 * @param jodaTimeContext the current JodaTimeContext,
	 * or {@code null} to reset the thread-bound context
	 */
	public static void setJodaTimeContext(JodaTimeContext jodaTimeContext) {
		if (jodaTimeContext == null) {
			resetJodaTimeContext();
		}
		else {
			jodaTimeContextHolder.set(jodaTimeContext);
		}
	}

	/**
	 * Return the JodaTimeContext associated with the current thread, if any.
	 * @return the current JodaTimeContext, or {@code null} if none
	 */
	public static JodaTimeContext getJodaTimeContext() {
		return jodaTimeContextHolder.get();
	}


	/**
	 * Obtain a DateTimeFormatter with user-specific settings applied to the given base Formatter.
	 * @param formatter the base formatter that establishes default formatting rules
	 * (generally user independent)
	 * @param locale the current user locale (may be {@code null} if not known)
	 * @return the user-specific DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		JodaTimeContext context = getJodaTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
