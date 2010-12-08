/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.env;

import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * TODO SPR-7508: document
 * TODO: Consider extracting a PropertyResolutionService interface
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface Environment {

	/**
	 * TODO SPR-7508: document
	 */
	Set<String> getActiveProfiles();

	/**
	 * TODO SPR-7508: document
	 */
	Set<String> getDefaultProfiles();

	/**
	 * TODO SPR-7508: document
	 * returns true if:
	 * 	a) one or more of specifiedProfiles are active in the given environment - see {@link #getActiveProfiles()}
	 *  b) specifiedProfiles contains default profile - see {@link #getDefaultProfile()}
	 */
	boolean acceptsProfiles(String[] specifiedProfiles);

	/**
	 * TODO SPR-7508: document
	 */
	boolean containsProperty(String key);

	/**
	 * TODO SPR-7508: document
	 */
	String getProperty(String key);

	/**
	 * TODO SPR-7508: document
	 */
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * TODO SPR-7508: document
	 */
	String getRequiredProperty(String key);

	/**
	 * TODO SPR-7508: document
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType);

	/**
	 * TODO SPR-7508: document
	 */
	int getPropertyCount();

	/**
	 * TODO SPR-7508: document
	 */
	Properties asProperties();

	/**
	 * TODO SPR-7508: document that this returns {@link System#getenv()} if allowed, or
	 * {@link ReadOnlySystemAttributesMap} if not.
	 */
	Map<String, String> getSystemEnvironment();

	/**
	 * TODO SPR-7508: document that this returns {@link System#getProperties()} if allowed, or
	 * {@link ReadOnlySystemAttributesMap} if not.  Actually, always returns
	 * {@link ReadOnlySystemAttributesMap} now.
	 * see notes within {@link AbstractEnvironment#getSystemProperties()}
	 */
	Map<String, String> getSystemProperties();

	/**
	 * TODO SPR-7508: document
	 * @see #resolveRequiredPlaceholders(String)
	 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String, int)
	 */
	String resolvePlaceholders(String text);

	/**
	 * TODO SPR-7508: document
	 * @see #resolvePlaceholders(String)
	 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String, int)
	 */
	String resolveRequiredPlaceholders(String path);

}
