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

import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link TimeZoneContext} interface,
 * always returning a specified {@code TimeZone}.
 *
 * @author Nicholas Williams
 * @since 4.0
 */
public class SimpleTimeZoneContext implements TimeZoneContext {

	private final TimeZone timeZone;


	/**
	 * Create a new SimpleTimeZoneContext that exposes the specified TimeZone.
	 * Every {@link #getTimeZone()} will return this Locale.
	 * @param timeZone the TimeZone to expose
	 */
	public SimpleTimeZoneContext(TimeZone timeZone) {
		Assert.notNull(timeZone, "Time zone must not be null");
		this.timeZone = timeZone;
	}


	@Override
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	@Override
	public String toString() {
		return this.timeZone.getID();
	}

}
