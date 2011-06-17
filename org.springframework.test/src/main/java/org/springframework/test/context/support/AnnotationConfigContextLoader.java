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

package org.springframework.test.context.support;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} which
 * registers bean definitions from
 * {@link org.springframework.context.annotation.Configuration configuration classes}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	/**
	 * TODO Document overridden processContextConfigurationAttributes().
	 *
	 * @see org.springframework.test.context.SmartContextLoader#processContextConfigurationAttributes
	 */
	public void processContextConfigurationAttributes(ContextConfigurationAttributes configAttributes) {
		if (ObjectUtils.isEmpty(configAttributes.getClasses()) && isGenerateDefaultClasses()) {
			Class<?>[] defaultConfigurationClasses = generateDefaultConfigurationClasses(configAttributes.getDeclaringClass());
			configAttributes.setClasses(defaultConfigurationClasses);
		}
	}

	/**
	 * TODO Update documentation regarding SmartContextLoader SPI.
	 * 
	 * <p>
	 * Registers {@link org.springframework.context.annotation.Configuration configuration classes}
	 * in the supplied {@link GenericApplicationContext context} from the specified
	 * class names.
	 * 
	 * <p>Each class name must be the <em>fully qualified class name</em> of an
	 * annotated configuration class, component, or feature specification. An
	 * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
	 * bean definitions.
	 * 
	 * <p>Note that this method does not call {@link #createBeanDefinitionReader}
	 * since <code>AnnotatedBeanDefinitionReader</code> is not an instance of
	 * {@link BeanDefinitionReader}.
	 * 
	 * @param context the context in which the configuration classes should be registered
	 * @param classNames the names of configuration classes to register in the context
	 * @throws IllegalArgumentException if a supplied class name does not represent a class
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context,
			MergedContextConfiguration mergedContextConfiguration) {
		Class<?>[] configClasses = mergedContextConfiguration.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering configuration classes: " + ObjectUtils.nullSafeToString(configClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(configClasses);
	}

	/**
	 * TODO Document isGenerateDefaultClasses().
	 */
	protected boolean isGenerateDefaultClasses() {
		return true;
	}

	/**
	 * Returns &quot;$ContextConfiguration</code>&quot;; intended to be used
	 * as a suffix to append to the name of the test class when generating
	 * default configuration class names.
	 * 
	 * <p>Note: the use of a dollar sign ($) signifies that the resulting 
	 * class name refers to a nested <code>static</code> class within the
	 * test class.
	 * 
	 * @see #generateDefaultLocations(Class)
	 */
	protected String getConfigurationClassNameSuffix() {
		return "$ContextConfiguration";
	}

	/**
	 * TODO Document generateDefaultConfigurationClasses().
	 */
	protected Class<?>[] generateDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");
		String suffix = getConfigurationClassNameSuffix();
		Assert.hasText(suffix, "Configuration class name suffix must not be empty");
		String className = declaringClass.getName() + suffix;

		List<Class<?>> configClasses = new ArrayList<Class<?>>();
		try {
			configClasses.add((Class<?>) getClass().getClassLoader().loadClass(className));
		}
		catch (ClassNotFoundException e) {
			logger.warn(String.format("Cannot load @Configuration class with generated class name [%s].", className), e);
		}

		return configClasses.toArray(new Class<?>[configClasses.size()]);
	}

	/**
	 * TODO Document overridden createBeanDefinitionReader().
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the createBeanDefinitionReader(GenericApplicationContext) method");
	}

	/**
	 * TODO Document overridden generateDefaultLocations().
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * TODO Document overridden modifyLocations().
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * TODO Document overridden getResourceSuffix().
	 */
	@Override
	protected String getResourceSuffix() {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the getResourceSuffix() method");
	}

}
