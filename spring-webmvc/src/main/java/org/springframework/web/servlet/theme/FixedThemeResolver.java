/*
 * Copyright 2002-2022 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * {@link org.springframework.web.servlet.ThemeResolver} implementation
 * that simply uses a fixed theme. The fixed name can be defined via
 * the "defaultThemeName" property; out of the box, it is "theme".
 *
 * <p>Note: Does not support {@code setThemeName}, as the fixed theme
 * cannot be changed.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 * @see #setDefaultThemeName
 * @deprecated as of 6.0 in favor of using CSS, without direct replacement
 */
@Deprecated(since = "6.0")
public class FixedThemeResolver extends AbstractThemeResolver {

	@Override
	public String resolveThemeName(HttpServletRequest request) {
		return getDefaultThemeName();
	}

	@Override
	public void setThemeName(
			HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable String themeName) {

		throw new UnsupportedOperationException("Cannot change theme - use a different theme resolution strategy");
	}

}
