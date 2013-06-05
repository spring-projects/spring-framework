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

import org.springframework.web.util.WebUtils;

/**
 * Implementation of TimeZoneResolver that uses a time zone attribute in the user's
 * session in case of a custom setting, with a fallback to the specified default
 * time zone.
 *
 * <p>This is most appropriate if the application needs user sessions anyway,
 * that is, when the HttpSession does not have to be created for the time zone.
 *
 * <p>Custom controllers can override the user's time zone by calling
 * {@code setTimeZone}, e.g. responding to a time zone change request.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see #setDefaultTimeZone
 * @see #setTimeZone
 */
public class SessionTimeZoneResolver extends AbstractTimeZoneResolver {

	/**
	 * Name of the session attribute that holds the time zone.
	 * Only used internally by this implementation.
	 * Use {@code RequestContext(Utils).getTimeZone()}
	 * to retrieve the current time zone in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone()
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTimeZone
	 */
	public static final String TIME_ZONE_SESSION_ATTRIBUTE_NAME =
			SessionTimeZoneResolver.class.getName() + ".TIME_ZONE";


	@Override
	public TimeZone resolveTimeZone(HttpServletRequest request) {
		TimeZone timeZone = (TimeZone) WebUtils.getSessionAttribute(request, TIME_ZONE_SESSION_ATTRIBUTE_NAME);
		if (timeZone != null) {
			return timeZone;
		}
		return this.getDefaultTimeZone();
	}

	@Override
	public void setTimeZone(HttpServletRequest request, HttpServletResponse response,
							TimeZone timeZone) {
		WebUtils.setSessionAttribute(request, TIME_ZONE_SESSION_ATTRIBUTE_NAME, timeZone);
	}

}
