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

package org.springframework.web.servlet.i18n;

import java.util.TimeZone;

import org.springframework.util.Assert;
import org.springframework.web.servlet.TimeZoneResolver;

/**
 * Abstract base class for {@link TimeZoneResolver} implementations.
 * Provides support for a default time zone.
 *
 * @author Nicholas Williams
 * @since 4.0
 */
public abstract class AbstractTimeZoneResolver implements TimeZoneResolver {

	/**
	 * Out-of-the-box value for the default time zone (the system default time zone).
	 */
	public final static TimeZone ORIGINAL_DEFAULT_TIME_ZONE = TimeZone.getDefault();


	private TimeZone defaultTimeZone = ORIGINAL_DEFAULT_TIME_ZONE;


	/**
	 * Set a default time zone that this resolver will fall back to if no other time zone
	 * is found.
	 *
	 * @param defaultTimeZone the default time zone to fall back to.
	 */
	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		Assert.notNull(defaultTimeZone, "The default time zone cannot be null.");
		this.defaultTimeZone = defaultTimeZone;
	}

	/**
	 * Return the default time zone that this resolver is supposed to fall back to, if
	 * any.
	 */
	protected TimeZone getDefaultTimeZone() {
		return this.defaultTimeZone;
	}

}
