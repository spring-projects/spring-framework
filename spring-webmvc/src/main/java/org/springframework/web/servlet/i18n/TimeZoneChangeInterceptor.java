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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.TimeZoneResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Interceptor that allows for changing the current time zone on every request,
 * via a configurable request parameter.
 *
 * @author Nicholas Williams
 * @since 4.0
 * @see org.springframework.web.servlet.TimeZoneResolver
 */
public class TimeZoneChangeInterceptor extends HandlerInterceptorAdapter {

	/**
	 * Default name of the time zone specification parameter: "timezone".
	 */
	public static final String DEFAULT_PARAM_NAME = "timezone";


	protected final Log logger = LogFactory.getLog(getClass());

	private String paramName = DEFAULT_PARAM_NAME;


	/**
	 * Set the name of the parameter that contains a time zone specification
	 * in a time zone change request. Default is "timezone".
	 */
	public void setParamName(String paramName) {
		Assert.hasLength(paramName, "The parameter name cannot be blank.");
		this.paramName = paramName;
	}

	/**
	 * Return the name of the parameter that contains a time zone specification
	 * in a time zone change request.
	 */
	public String getParamName() {
		return this.paramName;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {
		String newTimeZone = request.getParameter(this.paramName);
		if (newTimeZone != null) {
			TimeZone timeZone = StringUtils.parseTimeZoneString(newTimeZone);
			if (timeZone != null) {
				TimeZoneResolver timeZoneResolver = RequestContextUtils.getTimeZoneResolver(request);
				if (timeZoneResolver == null) {
					throw new IllegalStateException("No TimeZoneResolver found: not in a DispatcherServlet request?");
				}
				timeZoneResolver.setTimeZone(request, response, timeZone);
			}
			else if (logger.isInfoEnabled()) {
				logger.info("Failed to parse parameter value [" + newTimeZone +
						"] into time zone.");
			}
		}
		// Proceed in any case.
		return true;
	}

}
