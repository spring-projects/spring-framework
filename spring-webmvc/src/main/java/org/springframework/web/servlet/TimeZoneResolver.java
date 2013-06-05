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

package org.springframework.web.servlet;

import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface for web-based time zone resolution strategies that allows for
 * both time zone resolution via the request and time zone modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on request, session,
 * cookies, etc. There is no default implementation as there is no standard for
 * determining time zones from requests, but there are several implementations
 * in the {@link org.springframework.web.servlet.i18n} package.
 *
 * <p>Use {@link org.springframework.web.servlet.support.RequestContext#getTimeZone()}
 * to retrieve the current time zone in controllers or views, independent of the
 * actual resolution strategy.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see org.springframework.web.servlet.support.RequestContext#getTimeZone
 */
public interface TimeZoneResolver {

	/**
	 * Resolve the current time zone via the given request. Should return a default time
	 * zone as a fallback in any case.
	 * @param request the request to resolve the time zone for
	 * @return the current time zone (never {@code null})
	 */
	TimeZone resolveTimeZone(HttpServletRequest request);

	/**
	 * Set the current time zone to the given one.
	 * @param request the request to be used for time zone modification
	 * @param response the response to be used for time zone modification
	 * @param timeZone the new time zone, or {@code null} to clear the time zone
	 * @throws UnsupportedOperationException if the TimeZoneResolver implementation
	 * does not support dynamic changing of the theme
	 */
	void setTimeZone(HttpServletRequest request, HttpServletResponse response,
					 TimeZone timeZone);

}
