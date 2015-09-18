/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

/**
 * {@link ThemeSource} implementation that looks up an individual
 * {@link java.util.ResourceBundle} per theme. The theme name gets
 * interpreted as ResourceBundle basename, supporting a common
 * basename prefix for all themes.
 *
 * @author Jean-Pierre Pawlak
 * @author Juergen Hoeller
 * @see #setBasenamePrefix
 * @see java.util.ResourceBundle
 * @see org.springframework.context.support.ResourceBundleMessageSource
 */
public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private ThemeSource parentThemeSource;

	private String basenamePrefix = "";

	private String defaultEncoding;

	private Boolean fallbackToSystemLocale;

	private ClassLoader beanClassLoader;

	/** Map from theme name to Theme instance */
	private final Map<String, Theme> themeCache = new ConcurrentHashMap<String, Theme>();


	@Override
	public void setParentThemeSource(ThemeSource parent) {
		this.parentThemeSource = parent;

		// Update existing Theme objects.
		// Usually there shouldn't be any at the time of this call.
		synchronized (this.themeCache) {
			for (Theme theme : this.themeCache.values()) {
				initParent(theme);
			}
		}
	}

	@Override
	public ThemeSource getParentThemeSource() {
		return this.parentThemeSource;
	}

	/**
	 * Set the prefix that gets applied to the ResourceBundle basenames,
	 * i.e. the theme names.
	 * E.g.: basenamePrefix="test.", themeName="theme" -> basename="test.theme".
	 * <p>Note that ResourceBundle names are effectively classpath locations: As a
	 * consequence, the JDK's standard ResourceBundle treats dots as package separators.
	 * This means that "test.theme" is effectively equivalent to "test/theme",
	 * just like it is for programmatic {@code java.util.ResourceBundle} usage.
	 * @see java.util.ResourceBundle#getBundle(String)
	 */
	public void setBasenamePrefix(String basenamePrefix) {
		this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
	}

	/**
	 * Set the default charset to use for parsing resource bundle files.
	 * <p>{@link ResourceBundleMessageSource}'s default is the
	 * {@code java.util.ResourceBundle} default encoding: ISO-8859-1.
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setDefaultEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Set whether to fall back to the system Locale if no files for a
	 * specific Locale have been found.
	 * <p>{@link ResourceBundleMessageSource}'s default is "true".
	 * @since 4.2
	 * @see ResourceBundleMessageSource#setFallbackToSystemLocale
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * This implementation returns a SimpleTheme instance, holding a
	 * ResourceBundle-based MessageSource whose basename corresponds to
	 * the given theme name (prefixed by the configured "basenamePrefix").
	 * <p>SimpleTheme instances are cached per theme name. Use a reloadable
	 * MessageSource if themes should reflect changes to the underlying files.
	 * @see #setBasenamePrefix
	 * @see #createMessageSource
	 */
	@Override
	public Theme getTheme(String themeName) {
		if (themeName == null) {
			return null;
		}
		Theme theme = this.themeCache.get(themeName);
		if (theme == null) {
			synchronized (this.themeCache) {
				theme = this.themeCache.get(themeName);
				if (theme == null) {
					String basename = this.basenamePrefix + themeName;
					MessageSource messageSource = createMessageSource(basename);
					theme = new SimpleTheme(themeName, messageSource);
					initParent(theme);
					this.themeCache.put(themeName, theme);
					if (logger.isDebugEnabled()) {
						logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
					}
				}
			}
		}
		return theme;
	}

	/**
	 * Create a MessageSource for the given basename,
	 * to be used as MessageSource for the corresponding theme.
	 * <p>Default implementation creates a ResourceBundleMessageSource.
	 * for the given basename. A subclass could create a specifically
	 * configured ReloadableResourceBundleMessageSource, for example.
	 * @param basename the basename to create a MessageSource for
	 * @return the MessageSource
	 * @see org.springframework.context.support.ResourceBundleMessageSource
	 * @see org.springframework.context.support.ReloadableResourceBundleMessageSource
	 */
	protected MessageSource createMessageSource(String basename) {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename(basename);
		if (this.defaultEncoding != null) {
			messageSource.setDefaultEncoding(this.defaultEncoding);
		}
		if (this.fallbackToSystemLocale != null) {
			messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
		}
		if (this.beanClassLoader != null) {
			messageSource.setBeanClassLoader(this.beanClassLoader);
		}
		return messageSource;
	}

	/**
	 * Initialize the MessageSource of the given theme with the
	 * one from the corresponding parent of this ThemeSource.
	 * @param theme the Theme to (re-)initialize
	 */
	protected void initParent(Theme theme) {
		if (theme.getMessageSource() instanceof HierarchicalMessageSource) {
			HierarchicalMessageSource messageSource = (HierarchicalMessageSource) theme.getMessageSource();
			if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
				Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
				if (parentTheme != null) {
					messageSource.setParentMessageSource(parentTheme.getMessageSource());
				}
			}
		}
	}

}
