/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that reads
 * bean definitions from XML resources.
 *
 * <p>Default resource locations are detected using the suffix
 * {@code "-context.xml"}.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see XmlBeanDefinitionReader
 * @see GenericGroovyXmlContextLoader
 * @see AnnotationConfigContextLoader
 */
public class GenericXmlContextLoader extends AbstractGenericContextLoader {

	/**
	 * Create a new {@link XmlBeanDefinitionReader}.
	 * @return a new {@code XmlBeanDefinitionReader}
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return new XmlBeanDefinitionReader(context);
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
	 * Ensure that the supplied {@link MergedContextConfiguration} does not
	 * contain {@link MergedContextConfiguration#getClasses() classes}.
	 * @since 4.0.4
	 * @see AbstractGenericContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		if (mergedConfig.hasClasses()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, "
						+ "but %s does not support annotated classes.", mergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(mergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
