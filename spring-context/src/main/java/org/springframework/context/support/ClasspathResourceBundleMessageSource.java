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

package org.springframework.context.support;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.context.MessageSource} implementation that
 * accesses resource bundles using specified ant style <b>classpath:</b> and <b>classpath*:</b> class path search
 * patterns. This class relies on the underlying Spring's
 * {@link org.springframework.context.support.ResourceBundleMessageSource} implementation.
 * 
 * @author Movin K
 * 
 */
public class ClasspathResourceBundleMessageSource extends ResourceBundleMessageSource {

	private static final String PROPERTIES_FILE_SUFFIX = ".properties";
	private static final String XML_FILE_SUFFIX = ".xml";

	private static final Pattern SPRING_CLASSPATH_URL_PREFIX_PATTERN = Pattern.compile("^classpath(?:\\*)?:.+");
	private static final Pattern CLASSPATH_SEARCH_PATTERN_FILTER = Pattern.compile("^classpath(?:\\*)?:((?:/)?[^\\*]+)(?:\\*|).+$");

	/**
	 * Set a single basename if the base name follows {@link java.util.ResourceBundle} convention.
	 * If it follows ant style <b>classpath:</b> and <b>classpath*:</b> pattern, collection of
	 * resources files resulted by the classpath search will be set. When using ant style search patterns,
	 * resources files should be placed under a sub folder instead of the root folder as underline search
	 * mechanism is not reliable when the files are under the root folder. Also When using classpath search patterns,
	 * the order in which the resources files are loaded is not guaranteed.
	 * 
	 * For more details about {@link java.util.ResourceBundle} conventions,
	 * refer to {@link ResourceBundleMessageSource#setBasename(String)}
	 */
	@Override
	public void setBasename(String basename) {
		setBasenames(basename);
	}

	/**
	 * Set an array of basenames, each following {@link java.util.ResourceBundle} or
	 * ant style <b>classpath:</b> and <b>classpath*:</b> search patterns. When using ant style search patterns,
	 * resources files should be placed under a sub folder instead of the root folder as underline search
	 * mechanism is not reliable when the files are under the root folder. Also When using class path search patterns,
	 * the order in which the resources files are loaded is not guaranteed.
	 * 
	 * For more details about {@link java.util.ResourceBundle} conventions,
	 * refer to {@link ResourceBundleMessageSource#setBasenames(String...)}
	 */
	@Override
	public void setBasenames(String... basenames) {
		if (basenames != null) {
			Set<String> basenameSet = new HashSet<String>();
			for (String basename : basenames) {
				if (SPRING_CLASSPATH_URL_PREFIX_PATTERN.matcher(basename).matches()) {
					try {
						basenameSet.addAll(getBaseNamesFromClasspath(basename));
					} catch (IOException ex) {
						logger.warn("Error loading the resource files for [ " + basename + " ]", ex);
					}
				} else {
					basenameSet.add(basename);
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Returning basename set [ " + StringUtils.collectionToCommaDelimitedString(basenameSet) + " ]");
			}

			super.setBasenames(basenameSet.toArray(new String[] {}));
		}
	}

	private Set<String> getBaseNamesFromClasspath(String searchPattern) throws IOException {
		Set<String> basenameSet = new HashSet<String>();
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		String resourcesBaseName;
		for (Resource resource : resourcePatternResolver.getResources(searchPattern + PROPERTIES_FILE_SUFFIX)) {
			resourcesBaseName = getResourcesBaseNameBySeachPattern(resource.getURL().toExternalForm(), searchPattern);
			if (resourcesBaseName != null) {
				basenameSet.add(resourcesBaseName);
			}
		}

		for (Resource resource : resourcePatternResolver.getResources(searchPattern + XML_FILE_SUFFIX)) {
			resourcesBaseName = getResourcesBaseNameBySeachPattern(resource.getURL().toExternalForm(), searchPattern);
			if (resourcesBaseName != null) {
				basenameSet.add(resourcesBaseName);
			}
		}

		if(logger.isDebugEnabled()) {
			logger.debug("Returning basename set [ " + StringUtils.collectionToCommaDelimitedString(basenameSet) +
					" ] for seach pattern " + searchPattern);
		}
		
		return basenameSet;
	}

	private String getResourcesBaseNameBySeachPattern(String externalResourceUrl, String searchPattern) {
		if(logger.isDebugEnabled()) {
			logger.debug("Resources file found in classpath: [ " + externalResourceUrl + " ]");
		}
		
		Matcher matcher = CLASSPATH_SEARCH_PATTERN_FILTER.matcher(searchPattern);
		if (matcher.matches()) {
			int startIndex = externalResourceUrl.lastIndexOf(matcher.group(1));
			int lastIndex = externalResourceUrl.lastIndexOf(".");
			
			return externalResourceUrl.substring(startIndex, lastIndex);
		}

		return null;
	}

}
