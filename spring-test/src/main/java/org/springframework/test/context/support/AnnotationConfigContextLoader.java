/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that loads
 * bean definitions from annotated classes.
 *
 * <p>See the Javadoc for
 * {@link org.springframework.test.context.ContextConfiguration @ContextConfiguration}
 * for a definition of <em>annotated class</em>.
 *
 * <p>Note: {@code AnnotationConfigContextLoader} supports <em>annotated classes</em>
 * rather than the String-based resource locations defined by the legacy
 * {@link org.springframework.test.context.ContextLoader ContextLoader} API. Thus,
 * although {@code AnnotationConfigContextLoader} extends
 * {@code AbstractGenericContextLoader}, {@code AnnotationConfigContextLoader}
 * does <em>not</em> support any String-based methods defined by
 * {@code AbstractContextLoader} or {@code AbstractGenericContextLoader}.
 * Consequently, {@code AnnotationConfigContextLoader} should chiefly be
 * considered a {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * rather than a {@link org.springframework.test.context.ContextLoader ContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)
 * @see GenericXmlContextLoader
 * @see GenericGroovyXmlContextLoader
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	// --- SmartContextLoader -----------------------------------------------

	/**
	 * Process <em>annotated classes</em> in the supplied {@link ContextConfigurationAttributes}.
	 *
	 * <p>If the <em>annotated classes</em> are {@code null} or empty and
	 * {@link #isGenerateDefaultLocations()} returns {@code true}, this
	 * {@code SmartContextLoader} will attempt to {@link
	 * #detectDefaultConfigurationClasses detect default configuration classes}.
	 * If defaults are detected they will be
	 * {@link ContextConfigurationAttributes#setClasses(Class[]) set} in the
	 * supplied configuration attributes. Otherwise, properties in the supplied
	 * configuration attributes will not be modified.
	 *
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

	// --- AnnotationConfigContextLoader ---------------------------------------

	/**
	 * Detect the default configuration classes for the supplied test class.
	 *
	 * <p>The default implementation simply delegates to
	 * {@link AnnotationConfigContextLoaderUtils#detectDefaultConfigurationClasses(Class)}.
	 *
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never {@code null}
	 * @see AnnotationConfigContextLoaderUtils
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		return AnnotationConfigContextLoaderUtils.detectDefaultConfigurationClasses(declaringClass);
	}

	// --- AbstractContextLoader -----------------------------------------------

	/**
	 * {@code AnnotationConfigContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 *
	 * @see AbstractContextLoader#modifyLocations
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 *
	 * @see AbstractContextLoader#generateDefaultLocations
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * {@code AnnotationConfigContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 *
	 * @see AbstractContextLoader#getResourceSuffix
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the getResourceSuffix() method");
	}

	// --- AbstractGenericContextLoader ----------------------------------------

	/**
	 * Ensure that the supplied {@link MergedContextConfiguration} does not
	 * contain {@link MergedContextConfiguration#getLocations() locations}.
	 * @since 4.0.4
	 * @see AbstractGenericContextLoader#validateMergedContextConfiguration
	 */
	@Override
	protected void validateMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		if (mergedConfig.hasLocations()) {
			String msg = String.format(
				"Test class [%s] has been configured with @ContextConfiguration's 'locations' (or 'value') attribute %s, "
						+ "but %s does not support resource locations.", mergedConfig.getTestClass().getName(),
				ObjectUtils.nullSafeToString(mergedConfig.getLocations()), getClass().getSimpleName());
			logger.error(msg);
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * Register classes in the supplied {@link GenericApplicationContext context}
	 * from the classes in the supplied {@link MergedContextConfiguration}.
	 *
	 * <p>Each class must represent an <em>annotated class</em>. An
	 * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
	 * bean definitions.
	 *
	 * <p>Note that this method does not call {@link #createBeanDefinitionReader}
	 * since {@code AnnotatedBeanDefinitionReader} is not an instance of
	 * {@link BeanDefinitionReader}.
	 *
	 * @param context the context in which the annotated classes should be registered
	 * @param mergedConfig the merged configuration from which the classes should be retrieved
	 *
	 * @see AbstractGenericContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?>[] annotatedClasses = mergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering annotated classes: " + ObjectUtils.nullSafeToString(annotatedClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(annotatedClasses);
	}

	/**
	 * {@code AnnotationConfigContextLoader} should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 *
	 * @see #loadBeanDefinitions
	 * @see AbstractGenericContextLoader#createBeanDefinitionReader
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the createBeanDefinitionReader(GenericApplicationContext) method");
	}

}
