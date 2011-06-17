/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * TODO [SPR-8386] Document MergedContextConfiguration.
 * 
 * @author Sam Brannen
 * @since 3.1
 * @see ContextConfiguration
 * @see ActiveProfiles
 */
public class MergedContextConfiguration {

	private final Class<?> testClass;

	private final String[] locations;

	private final Class<?>[] classes;

	private final String[] activeProfiles;

	private final ContextLoader contextLoader;

	private final String contextKey;


	private static String[] processLocations(String[] locations) {
		return locations == null ? new String[] {} : locations;
	}

	private static Class<?>[] processClasses(Class<?>[] classes) {
		return classes == null ? new Class<?>[] {} : classes;
	}

	private static String[] processActiveProfiles(String[] activeProfiles) {
		if (activeProfiles == null) {
			return new String[] {};
		}

		// Active profiles must be unique and sorted in order to support proper
		// cache key generation. Specifically, profile sets {foo,bar} and
		// {bar,foo} must both result in the same array (e.g., [bar,foo]).
		SortedSet<String> sortedProfilesSet = new TreeSet<String>(Arrays.asList(activeProfiles));
		return StringUtils.toStringArray(sortedProfilesSet);
	}

	/**
	 * Generates a context <code>key</code> from the supplied values.
	 */
	private static String generateContextKey(String[] locations, Class<?>[] classes, String[] activeProfiles,
			ContextLoader contextLoader) {

		String locationsKey = ObjectUtils.nullSafeToString(locations);
		String classesKey = ObjectUtils.nullSafeToString(classes);
		String activeProfilesKey = ObjectUtils.nullSafeToString(activeProfiles);
		String contextLoaderKey = contextLoader == null ? "null" : contextLoader.getClass().getName();

		return String.format("locations = [%s], classes = [%s], activeProfiles = [%s], contextLoader = [%s]",
			locationsKey, classesKey, activeProfilesKey, contextLoaderKey);
	}

	/**
	 * TODO Document MergedContextConfiguration constructor.
	 */
	public MergedContextConfiguration(Class<?> testClass, String[] locations, Class<?>[] classes,
			String[] activeProfiles, ContextLoader contextLoader) {
		this.testClass = testClass;
		this.locations = processLocations(locations);
		this.classes = processClasses(classes);
		this.activeProfiles = processActiveProfiles(activeProfiles);
		this.contextLoader = contextLoader;
		this.contextKey = generateContextKey(this.locations, this.classes, this.activeProfiles, this.contextLoader);
	}

	/**
	 * TODO Document getTestClass().
	 */
	public Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * TODO Document getLocations().
	 */
	public String[] getLocations() {
		return this.locations;
	}

	/**
	 * TODO Document getClasses().
	 */
	public Class<?>[] getClasses() {
		return this.classes;
	}

	/**
	 * TODO Document getActiveProfiles().
	 */
	public String[] getActiveProfiles() {
		return this.activeProfiles;
	}

	/**
	 * TODO Document getContextLoader().
	 */
	public ContextLoader getContextLoader() {
		return this.contextLoader;
	}

	/**
	 * TODO Document getContextKey().
	 */
	public String getContextKey() {
		return this.contextKey;
	}

	/**
	 * TODO Document overridden toString().
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", this.testClass)//
		.append("locations", ObjectUtils.nullSafeToString(this.locations))//
		.append("classes", ObjectUtils.nullSafeToString(this.classes))//
		.append("activeProfiles", ObjectUtils.nullSafeToString(this.activeProfiles))//
		.append("contextLoader", this.contextLoader)//
		.append("contextKey", this.contextKey)//
		.toString();
	}

}
