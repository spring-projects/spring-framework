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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;

/**
 * Simple holder class that associates a LocaleContext instance
 * with the current thread. The LocaleContext will be inherited
 * by any child threads spawned by the current thread if the
 * {@code inheritable} flag is set to {@code true}.
 *
 * <p>Used as a central holder for the current Locale in Spring,
 * wherever necessary: for example, in MessageSourceAccessor.
 * DispatcherServlet automatically exposes its current Locale here.
 * Other applications can expose theirs too, to make classes like
 * MessageSourceAccessor automatically use that Locale.
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @since 1.2
 * @see LocaleContext
 * @see org.springframework.context.support.MessageSourceAccessor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public abstract class LocaleContextHolder {

	private static final ThreadLocal<LocaleContext> localeContextHolder =
			new NamedThreadLocal<LocaleContext>("Locale context");

	private static final ThreadLocal<LocaleContext> inheritableLocaleContextHolder =
			new NamedInheritableThreadLocal<LocaleContext>("Locale context");


	/**
	 * Reset the LocaleContext for the current thread.
	 */
	public static void resetLocaleContext() {
		localeContextHolder.remove();
		inheritableLocaleContextHolder.remove();
	}

	/**
	 * Associate the given LocaleContext with the current thread,
	 * <i>not</i> exposing it as inheritable for child threads.
	 * <p>The given LocaleContext may be a {@link TimeZoneAwareLocaleContext},
	 * containing a locale with associated time zone information.
	 * @param localeContext the current LocaleContext,
	 * or {@code null} to reset the thread-bound context
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(LocaleContext localeContext) {
		setLocaleContext(localeContext, false);
	}

	/**
	 * Associate the given LocaleContext with the current thread.
	 * <p>The given LocaleContext may be a {@link TimeZoneAwareLocaleContext},
	 * containing a locale with associated time zone information.
	 * @param localeContext the current LocaleContext,
	 * or {@code null} to reset the thread-bound context
	 * @param inheritable whether to expose the LocaleContext as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 * @see SimpleLocaleContext
	 * @see SimpleTimeZoneAwareLocaleContext
	 */
	public static void setLocaleContext(LocaleContext localeContext, boolean inheritable) {
		if (localeContext == null) {
			resetLocaleContext();
		}
		else {
			if (inheritable) {
				inheritableLocaleContextHolder.set(localeContext);
				localeContextHolder.remove();
			}
			else {
				localeContextHolder.set(localeContext);
				inheritableLocaleContextHolder.remove();
			}
		}
	}

	/**
	 * Return the LocaleContext associated with the current thread, if any.
	 * @return the current LocaleContext, or {@code null} if none
	 */
	public static LocaleContext getLocaleContext() {
		LocaleContext localeContext = localeContextHolder.get();
		if (localeContext == null) {
			localeContext = inheritableLocaleContextHolder.get();
		}
		return localeContext;
	}

	/**
	 * Associate the given Locale with the current thread,
	 * preserving any TimeZone that may have been set already.
	 * <p>Will implicitly create a LocaleContext for the given Locale,
	 * <i>not</i> exposing it as inheritable for child threads.
	 * @param locale the current Locale, or {@code null} to reset
	 * the locale part of thread-bound context
	 * @see #setTimeZone(TimeZone)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(Locale locale) {
		setLocale(locale, false);
	}

	/**
	 * Associate the given Locale with the current thread,
	 * preserving any TimeZone that may have been set already.
	 * <p>Will implicitly create a LocaleContext for the given Locale.
	 * @param locale the current Locale, or {@code null} to reset
	 * the locale part of thread-bound context
	 * @param inheritable whether to expose the LocaleContext as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 * @see #setTimeZone(TimeZone, boolean)
	 * @see SimpleLocaleContext#SimpleLocaleContext(Locale)
	 */
	public static void setLocale(Locale locale, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * Return the Locale associated with the current thread, if any,
	 * or the system default Locale otherwise. This is effectively a
	 * replacement for {@link java.util.Locale#getDefault()},
	 * able to optionally respect a user-level Locale setting.
	 * <p>Note: This method has a fallback to the system default Locale.
	 * If you'd like to check for the raw LocaleContext content
	 * (which may indicate no specific locale through {@code null}, use
	 * {@link #getLocaleContext()} and call {@link LocaleContext#getLocale()}
	 * @return the current Locale, or the system default Locale if no
	 * specific Locale has been associated with the current thread
	 * @see LocaleContext#getLocale()
	 * @see java.util.Locale#getDefault()
	 */
	public static Locale getLocale() {
		LocaleContext localeContext = getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null) {
				return locale;
			}
		}
		return Locale.getDefault();
	}

	/**
	 * Associate the given TimeZone with the current thread,
	 * preserving any Locale that may have been set already.
	 * <p>Will implicitly create a LocaleContext for the given Locale,
	 * <i>not</i> exposing it as inheritable for child threads.
	 * @param timeZone the current TimeZone, or {@code null} to reset
	 * the time zone part of the thread-bound context
	 * @see #setLocale(Locale)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(TimeZone timeZone) {
		setTimeZone(timeZone, false);
	}

	/**
	 * Associate the given TimeZone with the current thread,
	 * preserving any Locale that may have been set already.
	 * <p>Will implicitly create a LocaleContext for the given Locale.
	 * @param timeZone the current TimeZone, or {@code null} to reset
	 * the time zone part of the thread-bound context
	 * @param inheritable whether to expose the LocaleContext as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 * @see #setLocale(Locale, boolean)
	 * @see SimpleTimeZoneAwareLocaleContext#SimpleTimeZoneAwareLocaleContext(Locale, TimeZone)
	 */
	public static void setTimeZone(TimeZone timeZone, boolean inheritable) {
		LocaleContext localeContext = getLocaleContext();
		Locale locale = (localeContext != null ? localeContext.getLocale() : null);
		if (timeZone != null) {
			localeContext = new SimpleTimeZoneAwareLocaleContext(locale, timeZone);
		}
		else if (locale != null) {
			localeContext = new SimpleLocaleContext(locale);
		}
		else {
			localeContext = null;
		}
		setLocaleContext(localeContext, inheritable);
	}

	/**
	 * Return the TimeZone associated with the current thread, if any,
	 * or the system default TimeZone otherwise. This is effectively a
	 * replacement for {@link java.util.TimeZone#getDefault()},
	 * able to optionally respect a user-level TimeZone setting.
	 * <p>Note: This method has a fallback to the system default TimeZone.
	 * If you'd like to check for the raw LocaleContext content
	 * (which may indicate no specific time zone through {@code null}, use
	 * {@link #getLocaleContext()} and call {@link TimeZoneAwareLocaleContext#getTimeZone()}
	 * after downcasting to {@link TimeZoneAwareLocaleContext}.
	 * @return the current TimeZone, or the system default TimeZone if no
	 * specific TimeZone has been associated with the current thread
	 * @see TimeZoneAwareLocaleContext#getTimeZone()
	 * @see java.util.TimeZone#getDefault()
	 */
	public static TimeZone getTimeZone() {
		LocaleContext localeContext = getLocaleContext();
		if (localeContext instanceof TimeZoneAwareLocaleContext) {
			TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
			if (timeZone != null) {
				return timeZone;
			}
		}
		return TimeZone.getDefault();
	}

}
