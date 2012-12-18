/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.web.servlet.theme;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.util.WebUtils;

/**
 * Implementation of ThemeResolver that uses a theme attribute in the user's
 * session in case of a custom setting, with a fallback to the default theme.
 * This is most appropriate if the application needs user sessions anyway.
 *
 * <p>Custom controllers can override the user's theme by calling
 * <code>setThemeName</code>, e.g. responding to a theme change request.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see #setThemeName
 */
public class SessionThemeResolver extends AbstractThemeResolver {

	/**
	 * Name of the session attribute that holds the theme name.
	 * Only used internally by this implementation.
	 * Use <code>RequestContext(Utils).getTheme()</code>
	 * to retrieve the current theme in controllers or views.
	 * @see org.springframework.web.servlet.support.RequestContext#getTheme
	 * @see org.springframework.web.servlet.support.RequestContextUtils#getTheme
	 */
	public static final String THEME_SESSION_ATTRIBUTE_NAME = SessionThemeResolver.class.getName() + ".THEME";

	public String resolveThemeName(HttpServletRequest request) {
		String theme = (String) WebUtils.getSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME);
		// specific theme, or fallback to default?
		return (theme != null ? theme : getDefaultThemeName());
	}

	public void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName) {
		WebUtils.setSessionAttribute(request, THEME_SESSION_ATTRIBUTE_NAME, themeName);
	}

}
