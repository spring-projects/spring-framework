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

package org.springframework.web.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface for web-based theme resolution strategies that allows for
 * both theme resolution via the request and theme modification via
 * request and response.
 *
 * <p>This interface allows for implementations based on session,
 * cookies, etc. The default implementation is FixedThemeResolver,
 * simply using a configured default theme.
 *
 * <p>Note that this resolver is only responsible for determining the
 * current theme name. The Theme instance for the resolved theme name
 * gets looked up by DispatcherServlet via the respective ThemeSource,
 * i.e. the current WebApplicationContext.
 *
 * <p>Use RequestContext.getTheme() to retrieve the current theme in
 * controllers or views, independent of the actual resolution strategy.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see org.springframework.web.servlet.theme.FixedThemeResolver
 * @see org.springframework.ui.context.Theme
 * @see org.springframework.ui.context.ThemeSource
 * @see org.springframework.web.servlet.support.RequestContext#getTheme
 */
public interface ThemeResolver {

  /**
   * Resolve the current theme name via the given request.
   * Should return a default theme as fallback in any case.
   * @param request request to be used for resolution
   * @return the current theme name
   */
	String resolveThemeName(HttpServletRequest request);

  /**
   * Set the current theme name to the given one.
   * @param request request to be used for theme name modification
   * @param response response to be used for theme name modification
   * @param themeName the new theme name
	 * @throws UnsupportedOperationException if the ThemeResolver implementation
	 * does not support dynamic changing of the theme
   */
	void setThemeName(HttpServletRequest request, HttpServletResponse response, String themeName);

}
