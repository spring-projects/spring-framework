/*
 * Copyright 2002-2012 the original author or authors.
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

import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

/**
 * A context that holds user-specific Joda Time settings such as the user's Chronology (calendar system) and time zone.
 * A {@code null} property value indicate the user has not specified a setting.
 *
 * @author Keith Donald
 * @since 3.0
 * @see JodaTimeContextHolder
 */
public class JodaTimeContext {

	private Chronology chronology;

	private DateTimeZone timeZone;


	/**
	 * Set the user's chronology.
	 */
	public void setChronology(Chronology chronology) {
		this.chronology = chronology;
	}

	/**
	 * The user's chronology (calendar system).
	 * Null if not specified.
	 */
	public Chronology getChronology() {
		return this.chronology;
	}

	/**
	 * Set the user's timezone.
	 */
	public void setTimeZone(DateTimeZone timeZone) {
		this.timeZone = timeZone;
	}

	/**
	 * The user's timezone.
	 * Null if not specified.
	 */
	public DateTimeZone getTimeZone() {
		return timeZone;
	}


	/**
	 * Gets the Formatter with the this context's settings applied to the base {@code formatter}.
	 * @param formatter the base formatter that establishes default formatting rules, generally context independent
	 * @return the context DateTimeFormatter
	 */
	public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
		if (this.chronology != null) {
			formatter = formatter.withChronology(this.chronology);
		}
		if (this.timeZone != null) {
			formatter = formatter.withZone(this.timeZone);
		}
		return formatter;
	}

}
