/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.ui.context;

/**
 * Interface to be implemented by objects that can resolve {@link Theme Themes}.
 * This enables parameterization and internationalization of messages
 * for a given 'theme'.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see Theme
 */
public interface ThemeSource {

	/**
	 * Return the Theme instance for the given theme name.
	 * <p>The returned Theme will resolve theme-specific messages, codes,
	 * file paths, etc (e.g. CSS and image files in a web environment).
	 * @param themeName the name of the theme
	 * @return the corresponding Theme, or {@code null} if none defined.
	 * Note that, by convention, a ThemeSource should at least be able to
	 * return a default Theme for the default theme name "theme" but may also
	 * return default Themes for other theme names.
	 * @see org.springframework.web.servlet.theme.AbstractThemeResolver#ORIGINAL_DEFAULT_THEME_NAME
	 */
	Theme getTheme(String themeName);

}
