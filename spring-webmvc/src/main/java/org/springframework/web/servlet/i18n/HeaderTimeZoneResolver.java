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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of TimeZoneResolver that uses an HTTP header to resolve the time zone
 * that the user wants to use. The default header is {@code Accept-Timezone}, but this
 * does not imply that this is a standard HTTP header. There is no standard HTTP time
 * zone header. This class uses {@code Accept-Timezone} as the default to follow the
 * convention set by the official HTTP header {@code Accept-Language} used by
 * {@link AcceptHeaderLocaleResolver}.
 *
 * <p>For HTTP clients to use a custom time zone header, they must be configured to do so.
 * You cannot do this with most browsers, so this resolver is most useful for endpoints
 * expected to be accessed by non-browser clients, such as web service endpoints.
 *
 * <p>Note: Does not support {@code setLocale}, since the header
 * can only be changed through changing the client's settings.
 *
 * @author Nicholas Williams
 * @since 4.0
 */
public class HeaderTimeZoneResolver extends AbstractTimeZoneResolver {

	/**
	 * The default header name, which is {@code Accept-Timezone}. This should not be
	 * construed to mean that this is a standard HTTP header; there is no standard HTTP
	 * time zone header.
	 */
	public static final String DEFAULT_HEADER_NAME = "Accept-Timezone";


	protected final Log logger = LogFactory.getLog(getClass());

	private String headerName = DEFAULT_HEADER_NAME;


	public String getHeaderName() {
		return headerName;
	}

	public void setHeaderName(String headerName) {
		Assert.hasLength(headerName, "The header name cannot be empty.");
		this.headerName = headerName;
	}

	@Override
	public TimeZone resolveTimeZone(HttpServletRequest request) {
		String timeZoneHeader = request.getHeader(this.headerName);
		if (timeZoneHeader != null) {
			TimeZone timeZone = StringUtils.parseTimeZoneString(timeZoneHeader);
			if (timeZone != null) {
				return timeZone;
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Failed to parse parameter value [" + timeZoneHeader +
						"] into time zone.");
			}
		}
		return this.getDefaultTimeZone();
	}

	@Override
	public void setTimeZone(HttpServletRequest request, HttpServletResponse response,
							TimeZone timeZone) {
		throw new UnsupportedOperationException(
				"Cannot change HTTP time zone header - use a different time zone resolution strategy");
	}

}
