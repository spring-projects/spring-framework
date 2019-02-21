/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Abstract base class for {@code MessageSource} implementations based on
 * resource bundle conventions, such as {@link ResourceBundleMessageSource}
 * and {@link ReloadableResourceBundleMessageSource}. Provides common
 * configuration methods and corresponding semantic definitions.
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see ResourceBundleMessageSource
 * @see ReloadableResourceBundleMessageSource
 */
public abstract class AbstractResourceBasedMessageSource extends AbstractMessageSource {

	private final Set<String> basenameSet = new LinkedHashSet<>(4);

	@Nullable
	private String defaultEncoding;

	private boolean fallbackToSystemLocale = true;

	private long cacheMillis = -1;


	/**
	 * Set a single basename, following the basic ResourceBundle convention
	 * of not specifying file extension or language codes. The resource location
	 * format is up to the specific {@code MessageSource} implementation.
	 * <p>Regular and XMl properties files are supported: e.g. "messages" will find
	 * a "messages.properties", "messages_en.properties" etc arrangement as well
	 * as "messages.xml", "messages_en.xml" etc.
	 * @param basename the single basename
	 * @see #setBasenames
	 * @see org.springframework.core.io.ResourceEditor
	 * @see java.util.ResourceBundle
	 */
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * Set an array of basenames, each following the basic ResourceBundle convention
	 * of not specifying file extension or language codes. The resource location
	 * format is up to the specific {@code MessageSource} implementation.
	 * <p>Regular and XMl properties files are supported: e.g. "messages" will find
	 * a "messages.properties", "messages_en.properties" etc arrangement as well
	 * as "messages.xml", "messages_en.xml" etc.
	 * <p>The associated resource bundles will be checked sequentially when resolving
	 * a message code. Note that message definitions in a <i>previous</i> resource
	 * bundle will override ones in a later bundle, due to the sequential lookup.
	 * <p>Note: In contrast to {@link #addBasenames}, this replaces existing entries
	 * with the given names and can therefore also be used to reset the configuration.
	 * @param basenames an array of basenames
	 * @see #setBasename
	 * @see java.util.ResourceBundle
	 */
	public void setBasenames(String... basenames) {
		this.basenameSet.clear();
		addBasenames(basenames);
	}

	/**
	 * Add the specified basenames to the existing basename configuration.
	 * <p>Note: If a given basename already exists, the position of its entry
	 * will remain as in the original set. New entries will be added at the
	 * end of the list, to be searched after existing basenames.
	 * @since 4.3
	 * @see #setBasenames
	 * @see java.util.ResourceBundle
	 */
	public void addBasenames(String... basenames) {
		if (!ObjectUtils.isEmpty(basenames)) {
			for (String basename : basenames) {
				Assert.hasText(basename, "Basename must not be empty");
				this.basenameSet.add(basename.trim());
			}
		}
	}

	/**
	 * Return this {@code MessageSource}'s basename set, containing entries
	 * in the order of registration.
	 * <p>Calling code may introspect this set as well as add or remove entries.
	 * @since 4.3
	 * @see #addBasenames
	 */
	public Set<String> getBasenameSet() {
		return this.basenameSet;
	}

	/**
	 * Set the default charset to use for parsing properties files.
	 * Used if no file-specific charset is specified for a file.
	 * <p>The effective default is the {@code java.util.Properties}
	 * default encoding: ISO-8859-1. A {@code null} value indicates
	 * the platform default encoding.
	 * <p>Only applies to classic properties files, not to XML files.
	 * @param defaultEncoding the default charset
	 */
	public void setDefaultEncoding(@Nullable String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * Return the default charset to use for parsing properties files, if any.
	 * @since 4.3
	 */
	@Nullable
	protected String getDefaultEncoding() {
		return this.defaultEncoding;
	}

	/**
	 * Set whether to fall back to the system Locale if no files for a specific
	 * Locale have been found. Default is "true"; if this is turned off, the only
	 * fallback will be the default file (e.g. "messages.properties" for
	 * basename "messages").
	 * <p>Falling back to the system Locale is the default behavior of
	 * {@code java.util.ResourceBundle}. However, this is often not desirable
	 * in an application server environment, where the system Locale is not relevant
	 * to the application at all: set this flag to "false" in such a scenario.
	 */
	public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
		this.fallbackToSystemLocale = fallbackToSystemLocale;
	}

	/**
	 * Return whether to fall back to the system Locale if no files for a specific
	 * Locale have been found.
	 * @since 4.3
	 */
	protected boolean isFallbackToSystemLocale() {
		return this.fallbackToSystemLocale;
	}

	/**
	 * Set the number of seconds to cache loaded properties files.
	 * <ul>
	 * <li>Default is "-1", indicating to cache forever (just like
	 * {@code java.util.ResourceBundle}).
	 * <li>A positive number will cache loaded properties files for the given
	 * number of seconds. This is essentially the interval between refresh checks.
	 * Note that a refresh attempt will first check the last-modified timestamp
	 * of the file before actually reloading it; so if files don't change, this
	 * interval can be set rather low, as refresh attempts will not actually reload.
	 * <li>A value of "0" will check the last-modified timestamp of the file on
	 * every message access. <b>Do not use this in a production environment!</b>
	 * </ul>
	 * <p><b>Note that depending on your ClassLoader, expiration might not work reliably
	 * since the ClassLoader may hold on to a cached version of the bundle file.</b>
	 * Prefer {@link ReloadableResourceBundleMessageSource} over
	 * {@link ResourceBundleMessageSource} in such a scenario, in combination with
	 * a non-classpath location.
	 */
	public void setCacheSeconds(int cacheSeconds) {
		this.cacheMillis = (cacheSeconds * 1000);
	}

	/**
	 * Set the number of milliseconds to cache loaded properties files.
	 * Note that it is common to set seconds instead: {@link #setCacheSeconds}.
	 * <ul>
	 * <li>Default is "-1", indicating to cache forever (just like
	 * {@code java.util.ResourceBundle}).
	 * <li>A positive number will cache loaded properties files for the given
	 * number of milliseconds. This is essentially the interval between refresh checks.
	 * Note that a refresh attempt will first check the last-modified timestamp
	 * of the file before actually reloading it; so if files don't change, this
	 * interval can be set rather low, as refresh attempts will not actually reload.
	 * <li>A value of "0" will check the last-modified timestamp of the file on
	 * every message access. <b>Do not use this in a production environment!</b>
	 * </ul>
	 * @since 4.3
	 * @see #setCacheSeconds
	 */
	public void setCacheMillis(long cacheMillis) {
		this.cacheMillis = cacheMillis;
	}

	/**
	 * Return the number of milliseconds to cache loaded properties files.
	 * @since 4.3
	 */
	protected long getCacheMillis() {
		return this.cacheMillis;
	}

}
