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

package org.springframework.context.i18n;

import java.util.TimeZone;

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;

/**
 * Simple holder class that associates a TimeZoneContext instance
 * with the current thread. The TimeZoneContext will be inherited
 * by any child threads spawned by the current thread if the
 * {@code inheritable} flag is set to {@code true}.
 *
 * <p>Used as a central holder for the current TimeZone in Spring,
 * wherever necessary.
 * DispatcherServlet automatically exposes its current TimeZone here.
 * Other applications can expose theirs too, to make other classes
 * automatically use that TimeZone.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see TimeZoneContext
 * @see org.springframework.context.support.MessageSourceAccessor
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public abstract class TimeZoneContextHolder {

	private static final ThreadLocal<TimeZoneContext> timeZoneContextHolder =
			new NamedThreadLocal<TimeZoneContext>("Time zone context");

	private static final ThreadLocal<TimeZoneContext> inheritableTimeZoneContextHolder =
			new NamedInheritableThreadLocal<TimeZoneContext>("Time zone context");


	/**
	 * Reset the TimeZoneContext for the current thread.
	 */
	public static void resetTimeZoneContext() {
		timeZoneContextHolder.remove();
		inheritableTimeZoneContextHolder.remove();
	}

	/**
	 * Associate the given TimeZoneContext with the current thread,
	 * <i>not</i> exposing it as inheritable for child threads.
	 * @param timeZoneContext the current TimeZoneContext
	 */
	public static void setTimeZoneContext(TimeZoneContext timeZoneContext) {
		setTimeZoneContext(timeZoneContext, false);
	}

	/**
	 * Associate the given TimeZone with the current thread.
	 * @param timeZoneContext the current TimeZoneContext,
	 * or {@code null} to reset the thread-bound context
	 * @param inheritable whether to expose the TimeZoneContext as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 */
	public static void setTimeZoneContext(TimeZoneContext timeZoneContext, boolean inheritable) {
		if (timeZoneContext == null) {
			resetTimeZoneContext();
		}
		else {
			if (inheritable) {
				inheritableTimeZoneContextHolder.set(timeZoneContext);
				timeZoneContextHolder.remove();
			}
			else {
				timeZoneContextHolder.set(timeZoneContext);
				inheritableTimeZoneContextHolder.remove();
			}
		}
	}

	/**
	 * Return the TimeZoneContext associated with the current thread, if any.
	 * @return the current TimeZoneContext, or {@code null} if none
	 */
	public static TimeZoneContext getTimeZoneContext() {
		TimeZoneContext timeZoneContext = timeZoneContextHolder.get();
		if (timeZoneContext == null) {
			timeZoneContext = inheritableTimeZoneContextHolder.get();
		}
		return timeZoneContext;
	}

	/**
	 * Associate the given TimeZone with the current thread.
	 * <p>Will implicitly create a TimeZoneContext for the given TimeZone,
	 * <i>not</i> exposing it as inheritable for child threads.
	 * @param timeZone the current TimeZone, or {@code null} to reset
	 * the thread-bound context
	 * @see SimpleTimeZoneContext#SimpleTimeZoneContext(TimeZone)
	 */
	public static void setTimeZone(TimeZone timeZone) {
		setTimeZone(timeZone, false);
	}

	/**
	 * Associate the given TimeZone with the current thread.
	 * <p>Will implicitly create a TimeZoneContext for the given TimeZone.
	 * @param timeZone the current TimeZone, or {@code null} to reset
	 * the thread-bound context
	 * @param inheritable whether to expose the TimeZoneContext as inheritable
	 * for child threads (using an {@link InheritableThreadLocal})
	 * @see SimpleTimeZoneContext#SimpleTimeZoneContext(TimeZone)
	 */
	public static void setTimeZone(TimeZone timeZone, boolean inheritable) {
		TimeZoneContext timeZoneContext =
				(timeZone != null ? new SimpleTimeZoneContext(timeZone) : null);
		setTimeZoneContext(timeZoneContext, inheritable);
	}

	/**
	 * Return the TimeZone associated with the current thread, if any,
	 * or the system default TimeZone otherwise.
	 * @return the current TimeZone, or the system default TimeZone if no
	 * specific TimeZone has been associated with the current thread
	 * @see TimeZoneContext#getTimeZone() ()
	 * @see TimeZone#getDefault()
	 */
	public static TimeZone getTimeZone() {
		TimeZoneContext timeZoneContext = getTimeZoneContext();
		return (timeZoneContext != null ? timeZoneContext.getTimeZone() : TimeZone.getDefault());
	}

}
