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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link org.springframework.web.servlet.TimeZoneResolver} implementation
 * that always returns a fixed default time zone. Default is the current
 * JVM's default time zone.
 *
 * <p>Note: Does not support {@code setTimeZone}, as the fixed time zone
 * cannot be changed.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see #setDefaultTimeZone
 */
public class FixedTimeZoneResolver extends AbstractTimeZoneResolver {

	/**
	 * Create a default FixedTimeZoneResolver, exposing a configured default
	 * time zone (or the JVM's default time zone as fallback).
	 * @see #setDefaultTimeZone(java.util.TimeZone)
	 */
	public FixedTimeZoneResolver() {

	}

	/**
	 * Create a FixedTimeZoneResolver that exposes the given time zone.
	 * @param timeZone the time zone to expose
	 */
	public FixedTimeZoneResolver(TimeZone timeZone) {
		setDefaultTimeZone(timeZone);
	}


	@Override
	public TimeZone resolveTimeZone(HttpServletRequest request) {
		TimeZone timeZone = getDefaultTimeZone();
		if (timeZone == null) {
			timeZone = TimeZone.getDefault();
		}
		return timeZone;
	}

	@Override
	public void setTimeZone(HttpServletRequest request, HttpServletResponse response,
							TimeZone timeZone) {
		throw new UnsupportedOperationException(
				"Cannot change fixed time zone - use a different time zone resolution strategy");
	}

}
