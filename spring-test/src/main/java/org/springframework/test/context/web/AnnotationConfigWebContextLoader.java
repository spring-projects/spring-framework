/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.web;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoaderUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * Concrete implementation of {@link AbstractGenericWebContextLoader} that loads
 * bean definitions from annotated classes.
 *
 * <p>See the Javadoc for
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}
 * for a definition of <em>annotated class</em>.
 *
 * <p>Note: {@code AnnotationConfigWebContextLoader} supports <em>annotated classes</em>
 * rather than the String-based resource locations defined by the legacy
 * {@link org.springframework.test.context.ContextLoader ContextLoader} API. Thus,
 * although {@code AnnotationConfigWebContextLoader} extends
 * {@code AbstractGenericWebContextLoader}, {@code AnnotationConfigWebContextLoader}
 * does <em>not</em> support any String-based methods defined by
 * {@link org.springframework.test.context.support.AbstractContextLoader
 * AbstractContextLoader} or {@code AbstractGenericWebContextLoader}.
 * Consequently, {@code AnnotationConfigWebContextLoader} should chiefly be
 * considered a {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * rather than a {@link org.springframework.test.context.ContextLoader ContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericWebApplicationContext, WebMergedContextConfiguration)
 * @see GenericXmlWebContextLoader
 * @see GenericGroovyXmlWebContextLoader
 */
public class AnnotationConfigWebContextLoader extends AbstractGenericWebContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigWebContextLoader.class);


	// SmartContextLoader

	/**
	 * Process <em>annotated classes</em> in the supplied {@link ContextConfigurationAttributes}.
	 * <p>If the <em>annotated classes</em> are {@code null} or empty and
	 * {@link #isGenerateDefaultLocations()} returns {@code true}, this
	 * {@code SmartContextLoader} will attempt to {@linkplain
	 * #detectDefaultConfigurationClasses detect default configuration classes}.
	 * If defaults are detected they will be
	 * {@linkplain ContextConfigurationAttributes#setClasses(Class[]) set} in the
	 * supplied configuration attributes. Otherwise, properties in the supplied
	 * configuration attributes will not be modified.
	 * @param configAttributes the context configuration attributes to process
	 * @see org.springframework.test.context.SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
	 * @see #isGenerateDefaultLocations()
	 * @see #detectDefaultConfigurationClasses(Class)
	 */
	@Override
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		if (!configAttributes.hasClasses() && isGenerateDefaultLocations()) {
			configAttributes.setClasses(detectDefaultConfigurationClasses(configAttributes.getDeclaringClass()));
		}
	}

	/**
	 * Detect the default configuration classes for the supplied test class.
	 * <p>The default implementation simply delegates to
	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but never {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}


	// AbstractContextLoader

	/**
	 * {@code AnnotationConfigWebContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @throws UnsupportedOperationException in this implementation
	 * @see org.springframework.test.context.support.AbstractContextLoader#modifyLocations
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @throws UnsupportedOperationException in this implementation
	 * @see org.springframework.test.context.support.AbstractContextLoader#generateDefaultLocations
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigWebContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @throws UnsupportedOperationException in this implementation
	 * @see org.springframework.test.context.support.AbstractContextLoader#getResourceSuffix
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
				"AnnotationConfigWebContextLoader does not support the getResourceSuffix() method");
	}


	// AbstractGenericWebContextLoader

	/**
	 * Register classes in the supplied {@linkplain GenericWebApplicationContext context}
	 * from the classes in the supplied {@link WebMergedContextConfiguration}.
	 * <p>Each class must represent an <em>annotated class</em>. An
	 * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
	 * bean definitions.
	 * @param context the context in which the annotated classes should be registered
	 * @param webMergedConfig the merged configuration from which the classes should be retrieved
	 * @see AbstractGenericWebContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(
			GenericWebApplicationContext context, WebMergedContextConfiguration webMergedConfig) {

		Class<?>[] annotatedClasses = webMergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering annotated classes: " + Arrays.toString(annotatedClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
	}

	/**
	 * Ensure that the supplied {@link WebMergedContextConfiguration} does not
	 * contain {@link MergedContextConfiguration#getLocations() locations}.
	 * @since 4.0.4
	 * @see AbstractGenericWebContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(WebMergedContextConfiguration webMergedConfig) {
		if (webMergedConfig.hasLocations()) {
			String msg = """
					Test class [%s] has been configured with @ContextConfiguration's 'locations' \
					(or 'value') attribute %s, but %s does not support resource locations."""
						.formatted(webMergedConfig.getTestClass().getName(),
							Arrays.toString(webMergedConfig.getLocations()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

}
