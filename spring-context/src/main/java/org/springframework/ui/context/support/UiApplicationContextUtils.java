/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.ui.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.ThemeSource;

/**
 * Utility class for UI application context implementations.
 * Provides support for a special bean named "themeSource",
 * of type {@link org.springframework.ui.context.ThemeSource}.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @since 17.06.2003
 */
public abstract class UiApplicationContextUtils {

	/**
	 * Name of the ThemeSource bean in the factory.
	 * If none is supplied, theme resolution is delegated to the parent.
	 * @see org.springframework.ui.context.ThemeSource
	 */
	public static final String THEME_SOURCE_BEAN_NAME = "themeSource";


	private static final Log logger = LogFactory.getLog(UiApplicationContextUtils.class);


	/**
	 * Initialize the ThemeSource for the given application context,
	 * autodetecting a bean with the name "themeSource". If no such
	 * bean is found, a default (empty) ThemeSource will be used.
	 * @param context current application context
	 * @return the initialized theme source (will never be {@code null})
	 * @see #THEME_SOURCE_BEAN_NAME
	 */
	public static ThemeSource initThemeSource(ApplicationContext context) {
		if (context.containsLocalBean(THEME_SOURCE_BEAN_NAME)) {
			ThemeSource themeSource = context.getBean(THEME_SOURCE_BEAN_NAME, ThemeSource.class);
			// Make ThemeSource aware of parent ThemeSource.
			if (context.getParent() instanceof ThemeSource && themeSource instanceof HierarchicalThemeSource hts) {
				if (hts.getParentThemeSource() == null) {
					// Only set parent context as parent ThemeSource if no parent ThemeSource
					// registered already.
					hts.setParentThemeSource((ThemeSource) context.getParent());
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Using ThemeSource [" + themeSource + "]");
			}
			return themeSource;
		}
		else {
			// Use default ThemeSource to be able to accept getTheme calls, either
			// delegating to parent context's default or to local ResourceBundleThemeSource.
			HierarchicalThemeSource themeSource = null;
			if (context.getParent() instanceof ThemeSource) {
				themeSource = new DelegatingThemeSource();
				themeSource.setParentThemeSource((ThemeSource) context.getParent());
			}
			else {
				themeSource = new ResourceBundleThemeSource();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to locate ThemeSource with name '" + THEME_SOURCE_BEAN_NAME +
						"': using default [" + themeSource + "]");
			}
			return themeSource;
		}
	}

}
