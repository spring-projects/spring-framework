/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.core.NamedThreadLocal;

/**
 * A holder for a thread-local user {@link DateTimeContext}.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 */
public final class DateTimeContextHolder {

	private static final ThreadLocal<DateTimeContext> dateTimeContextHolder =
			new NamedThreadLocal<>("DateTimeContext");


	private DateTimeContextHolder() {
	}


	/**
	 * Reset the DateTimeContext for the current thread.
	 */
	public static void resetDateTimeContext() {
		dateTimeContextHolder.remove();
	}

	/**
	 * Associate the given DateTimeContext with the current thread.
	 * @param dateTimeContext the current DateTimeContext,
	 * or {@code null} to reset the thread-bound context
	 */
	public static void setDateTimeContext(@Nullable DateTimeContext dateTimeContext) {
		if (dateTimeContext == null) {
			resetDateTimeContext();
		}
		else {
			dateTimeContextHolder.set(dateTimeContext);
		}
	}

	/**
	 * Return the DateTimeContext associated with the current thread, if any.
	 * @return the current DateTimeContext, or {@code null} if none
	 */
	public static @Nullable DateTimeContext getDateTimeContext() {
		return dateTimeContextHolder.get();
	}

	/**
	 * Obtain a DateTimeFormatter with user-specific settings applied to the given base formatter.
	 * @param formatter the base formatter that establishes default formatting rules
	 * (generally user independent)
	 * @param locale the current user locale (may be {@code null} if not known)
	 * @return the user-specific DateTimeFormatter
	 */
	public static DateTimeFormatter getFormatter(DateTimeFormatter formatter, @Nullable Locale locale) {
		DateTimeFormatter formatterToUse = (locale != null ? formatter.withLocale(locale) : formatter);
		DateTimeContext context = getDateTimeContext();
		return (context != null ? context.getFormatter(formatterToUse) : formatterToUse);
	}

}
