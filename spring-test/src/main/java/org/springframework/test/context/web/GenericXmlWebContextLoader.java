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

package org.springframework.test.context.web;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Concrete implementation of {@link AbstractGenericWebContextLoader} that loads
 * bean definitions from XML resources.
 *
 * <p>Default resource locations are detected using the suffix
 * {@code "-context.xml"}.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see GenericGroovyXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 */
public class GenericXmlWebContextLoader extends AbstractGenericWebContextLoader {

	/**
	 * Load bean definitions into the supplied {@link GenericWebApplicationContext context}
	 * from the locations in the supplied {@code WebMergedContextConfiguration}, using an
	 * {@link XmlBeanDefinitionReader}.
	 * @see AbstractGenericWebContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericWebApplicationContext context,
			WebMergedContextConfiguration webMergedConfig) {
		new XmlBeanDefinitionReader(context).loadBeanDefinitions(webMergedConfig.getLocations());
	}

	/**
	 * Returns {@code "-context.xml"} in order to support detection of a
	 * default XML config file.
	 */
	@Override
	protected String getResourceSuffix() {
		return "-context.xml";
	}

	/**
	 * Ensure that the supplied {@link WebMergedContextConfiguration} does not
	 * contain {@link MergedContextConfiguration#getClasses() classes}.
	 * @since 4.0.4
	 * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasClasses()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
						+ "but %s does not support annotated classes.", webMergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(webMergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
