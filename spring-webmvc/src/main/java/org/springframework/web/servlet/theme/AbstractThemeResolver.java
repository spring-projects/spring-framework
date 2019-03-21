/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.theme;

import org.springframework.web.servlet.ThemeResolver;

/**
 * Abstract base class for {@link ThemeResolver} implementations.
 * Provides support for a default theme name.
 *
 * @author Juergen Hoeller
 * @author Jean-Pierre Pawlak
 * @since 17.06.2003
 */
public abstract class AbstractThemeResolver implements ThemeResolver {

	/**
	 * Out-of-the-box value for the default theme name: "theme".
	 */
	public final static String ORIGINAL_DEFAULT_THEME_NAME = "theme";

	private String defaultThemeName = ORIGINAL_DEFAULT_THEME_NAME;


	/**
	 * Set the name of the default theme.
	 * Out-of-the-box value is "theme".
	 */
	public void setDefaultThemeName(String defaultThemeName) {
		this.defaultThemeName = defaultThemeName;
	}

	/**
	 * Return the name of the default theme.
	 */
	public String getDefaultThemeName() {
		return this.defaultThemeName;
	}

}
