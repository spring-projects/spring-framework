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

package org.springframework.ui.context.support;

import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * Empty ThemeSource that delegates all calls to the parent ThemeSource.
 * If no parent is available, it simply won't resolve any theme.
 *
 * <p>Used as placeholder by UiApplicationContextUtils, if a context doesn't
 * define its own ThemeSource. Not intended for direct use in applications.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see UiApplicationContextUtils
 */
public class DelegatingThemeSource implements HierarchicalThemeSource {

	private ThemeSource parentThemeSource;


	public void setParentThemeSource(ThemeSource parentThemeSource) {
		this.parentThemeSource = parentThemeSource;
	}

	public ThemeSource getParentThemeSource() {
		return parentThemeSource;
	}


	public Theme getTheme(String themeName) {
		if (this.parentThemeSource != null) {
			return this.parentThemeSource.getTheme(themeName);
		}
		else {
			return null;
		}
	}

}
