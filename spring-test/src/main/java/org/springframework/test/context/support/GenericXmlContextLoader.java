/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import java.util.Arrays;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;

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
			String msg = """
					Test class [%s] has been configured with @ContextConfiguration's 'classes' attribute %s, \
					but %s does not support annotated classes.""".formatted(mergedConfig.getTestClass().getName(),
						Arrays.toString(mergedConfig.getClasses()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
