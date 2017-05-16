/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Interceptor that allows for changing the current locale on every request,
 * via a configurable request parameter (default parameter name: "locale").
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 20.06.2003
 * @see org.springframework.web.servlet.LocaleResolver
 */
public class LocaleChangeInterceptor extends HandlerInterceptorAdapter {

	/**
	 * Default name of the locale specification parameter: "locale".
	 */
	public static final String DEFAULT_PARAM_NAME = "locale";


	protected final Log logger = LogFactory.getLog(getClass());

	private String paramName = DEFAULT_PARAM_NAME;

	private String[] httpMethods;

	private boolean ignoreInvalidLocale = false;

	private boolean languageTagCompliant = false;


	/**
	 * Set the name of the parameter that contains a locale specification
	 * in a locale change request. Default is "locale".
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * Return the name of the parameter that contains a locale specification
	 * in a locale change request.
	 */
	public String getParamName() {
		return this.paramName;
	}

	/**
	 * Configure the HTTP method(s) over which the locale can be changed.
	 * @param httpMethods the methods
	 * @since 4.2
	 */
	public void setHttpMethods(String... httpMethods) {
		this.httpMethods = httpMethods;
	}

	/**
	 * Return the configured HTTP methods.
	 * @since 4.2
	 */
	public String[] getHttpMethods() {
		return this.httpMethods;
	}

	/**
	 * Set whether to ignore an invalid value for the locale parameter.
	 * @since 4.2.2
	 */
	public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
		this.ignoreInvalidLocale = ignoreInvalidLocale;
	}

	/**
	 * Return whether to ignore an invalid value for the locale parameter.
	 * @since 4.2.2
	 */
	public boolean isIgnoreInvalidLocale() {
		return this.ignoreInvalidLocale;
	}

	/**
	 * Specify whether to parse request parameter values as BCP 47 language tags
	 * instead of Java's legacy locale specification format.
	 * The default is {@code false}.
	 * <p>Note: This mode requires JDK 7 or higher. Set this flag to {@code true}
	 * for BCP 47 compliance on JDK 7+ only.
	 * @since 4.3
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 */
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		this.languageTagCompliant = languageTagCompliant;
	}

	/**
	 * Return whether to use BCP 47 language tags instead of Java's legacy
	 * locale specification format.
	 * @since 4.3
	 */
	public boolean isLanguageTagCompliant() {
		return this.languageTagCompliant;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			if (checkHttpMethod(request.getMethod())) {
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					throw new IllegalStateException(
							"No LocaleResolver found: not in a DispatcherServlet request?");
				}
				try {
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
				}
				catch (IllegalArgumentException ex) {
					if (isIgnoreInvalidLocale()) {
						logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
					}
					else {
						throw ex;
					}
				}
			}
		}
		// Proceed in any case.
		return true;
	}

	private boolean checkHttpMethod(String currentMethod) {
		String[] configuredMethods = getHttpMethods();
		if (ObjectUtils.isEmpty(configuredMethods)) {
			return true;
		}
		for (String configuredMethod : configuredMethods) {
			if (configuredMethod.equalsIgnoreCase(currentMethod)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parse the given locale value as coming from a request parameter.
	 * <p>The default implementation calls {@link StringUtils#parseLocaleString(String)}
	 * or JDK 7's {@link Locale#forLanguageTag(String)}, depending on the
	 * {@link #setLanguageTagCompliant "languageTagCompliant"} configuration property.
	 * @param locale the locale value to parse
	 * @return the corresponding {@code Locale} instance
	 * @since 4.3
	 */
	protected Locale parseLocaleValue(String locale) {
		return (isLanguageTagCompliant() ? Locale.forLanguageTag(locale) : StringUtils.parseLocaleString(locale));
	}

}
