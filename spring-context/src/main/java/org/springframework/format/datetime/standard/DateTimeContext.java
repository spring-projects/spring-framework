/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.format.datetime.standard;

import java.time.ZoneId;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;

/**
 * A context that holds user-specific <code>java.time</code> (JSR-310) settings
 * such as the user's Chronology (calendar system) and time zone.
 * <p>A {@code null} property value indicates the user has not specified a setting.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see DateTimeContextHolder
 */
public class DateTimeContext {

	private @Nullable Chronology chronology;

	private @Nullable ZoneId timeZone;


	/**
	 * Set the user's chronology (calendar system).
	 */
	public void setChronology(@Nullable Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * Return the user's chronology (calendar system), if any.
	 */
	public @Nullable Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * Set the user's time zone.
	 * <p>Alternatively, set a {@link TimeZoneAwareLocaleContext} on
	 * {@link LocaleContextHolder}. This context class will fall back to
	 * checking the locale context if no setting has been provided here.
	 * @see org.springframework.context.i18n.LocaleContextHolder#getTimeZone()
	 * @see org.springframework.context.i18n.LocaleContextHolder#setLocaleContext
	 */
	public void setTimeZone(@Nullable ZoneId timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * Return the user's time zone, if any.
	 */
	public @Nullable ZoneId getTimeZone() {
		return this.timeZone;
	}


	/**
	 * Get the DateTimeFormatter with this context's settings applied to the
	 * base {@code formatter}.
	 * @param formatter the base formatter that establishes default
	 * formatting rules, generally context-independent
	 * @return the contextual DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		}
		else {
			LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
			if (localeContext instanceof TimeZoneAwareLocaleContext timeZoneAware) {
				TimeZone timeZone = timeZoneAware.getTimeZone();
				if (timeZone != null) {
					formatter = formatter.withZone(timeZone.toZoneId());
				}
			}
		}
		return formatter;
	}

}
