/*
 * Copyright 2002-2012 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} that loads
 * bean definitions from
 * {@link org.springframework.context.annotation.Configuration configuration classes}.
 *
 * <p>Note: <code>AnnotationConfigContextLoader</code> supports
 * {@link org.springframework.context.annotation.Configuration configuration classes}
 * rather than the String-based resource locations defined by the legacy
 * {@link org.springframework.test.context.ContextLoader ContextLoader} API. Thus,
 * although <code>AnnotationConfigContextLoader</code> extends
 * <code>AbstractGenericContextLoader</code>, <code>AnnotationConfigContextLoader</code>
 * does <em>not</em> support any String-based methods defined by
 * <code>AbstractContextLoader</code> or <code>AbstractGenericContextLoader</code>.
 * Consequently, <code>AnnotationConfigContextLoader</code> should chiefly be
 * considered a {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
 * rather than a {@link org.springframework.test.context.ContextLoader ContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see #processContextConfiguration(ContextConfigurationAttributes)
 * @see #detectDefaultConfigurationClasses(Class)
 * @see #loadBeanDefinitions(GenericApplicationContext, MergedContextConfiguration)
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	// --- SmartContextLoader -----------------------------------------------

	/**
	 * Process configuration classes in the supplied {@link ContextConfigurationAttributes}.
	 * <p>If the configuration classes are <code>null</code> or empty and
	 * {@link #isGenerateDefaultLocations()} returns <code>true</code>, this
	 * <code>SmartContextLoader</code> will attempt to {@link
	 * #detectDefaultConfigurationClasses detect default configuration classes}.
	 * If defaults are detected they will be
	 * {@link ContextConfigurationAttributes#setClasses(Class[]) set} in the
	 * supplied configuration attributes. Otherwise, properties in the supplied
	 * configuration attributes will not be modified.
	 * @param configAttributes the context configuration attributes to process
	 * @see org.springframework.test.context.SmartContextLoader#processContextConfiguration(ContextConfigurationAttributes)
	 * @see #isGenerateDefaultLocations()
	 * @see #detectDefaultConfigurationClasses(Class)
	 */
	public void processContextConfiguration(ContextConfigurationAttributes configAttributes) {
		if (ObjectUtils.isEmpty(configAttributes.getClasses()) && isGenerateDefaultLocations()) {
			Class<?>[] defaultConfigClasses = detectDefaultConfigurationClasses(configAttributes.getDeclaringClass());
			configAttributes.setClasses(defaultConfigClasses);
		}
	}

	// --- AnnotationConfigContextLoader ---------------------------------------

	private boolean isStaticNonPrivateAndNonFinal(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		int modifiers = clazz.getModifiers();
		return (Modifier.isStatic(modifiers) && !Modifier.isPrivate(modifiers) && !Modifier.isFinal(modifiers));
	}

	/**
	 * Determine if the supplied {@link Class} meets the criteria for being
	 * considered a <em>default configuration class</em> candidate.
	 * <p>Specifically, such candidates:
	 * <ul>
	 * <li>must not be <code>null</code></li>
	 * <li>must not be <code>private</code></li>
	 * <li>must not be <code>final</code></li>
	 * <li>must be <code>static</code></li>
	 * <li>must be annotated with {@code @Configuration}</li>
	 * </ul>
	 * @param clazz the class to check
	 * @return <code>true</code> if the supplied class meets the candidate criteria
	 */
	private boolean isDefaultConfigurationClassCandidate(Class<?> clazz) {
		return clazz != null && isStaticNonPrivateAndNonFinal(clazz) && clazz.isAnnotationPresent(Configuration.class);
	}

	/**
	 * Detect the default configuration classes for the supplied test class.
	 * <p>The returned class array will contain all static inner classes of
	 * the supplied class that meet the requirements for {@code @Configuration}
	 * class implementations as specified in the documentation for
	 * {@link Configuration @Configuration}.
	 * <p>The implementation of this method adheres to the contract defined in the
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader}
	 * SPI. Specifically, this method uses introspection to detect default
	 * configuration classes that comply with the constraints required of
	 * {@code @Configuration} class implementations. If a potential candidate
	 * configuration class does meet these requirements, this method will log a
	 * warning, and the potential candidate class will be ignored.
	 * @param declaringClass the test class that declared {@code @ContextConfiguration}
	 * @return an array of default configuration classes, potentially empty but
	 * never <code>null</code>
	 */
	protected Class<?>[] detectDefaultConfigurationClasses(Class<?> declaringClass) {
		Assert.notNull(declaringClass, "Declaring class must not be null");

		List<Class<?>> configClasses = new ArrayList<Class<?>>();

		for (Class<?> candidate : declaringClass.getDeclaredClasses()) {
			if (isDefaultConfigurationClassCandidate(candidate)) {
				configClasses.add(candidate);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format(
						"Ignoring class [%s]; it must be static, non-private, non-final, and annotated "
								+ "with @Configuration to be considered a default configuration class.",
						candidate.getName()));
				}
			}
		}

		if (configClasses.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Could not detect default configuration classes for test class [%s]: "
						+ "%s does not declare any static, non-private, non-final, inner classes "
						+ "annotated with @Configuration.", declaringClass.getName(), declaringClass.getSimpleName()));
			}
		}

		return configClasses.toArray(new Class<?>[configClasses.size()]);
	}

	// --- AbstractContextLoader -----------------------------------------------

	/**
	 * <code>AnnotationConfigContextLoader</code> should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @see AbstractContextLoader#modifyLocations
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the modifyLocations(Class, String...) method");
	}

	/**
	 * <code>AnnotationConfigContextLoader</code> should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
	 * @see AbstractContextLoader#generateDefaultLocations
	 * @throws UnsupportedOperationException
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		throw new UnsupportedOperationException(
			"AnnotationConfigContextLoader does not support the generateDefaultLocations(Class) method");
	}

	/**
	 * <code>AnnotationConfigContextLoader</code> should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
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
	 * Register {@link org.springframework.context.annotation.Configuration configuration classes}
	 * in the supplied {@link GenericApplicationContext context} from the classes
	 * in the supplied {@link MergedContextConfiguration}.
	 * <p>Each class must represent an annotated configuration class or component. An
	 * {@link AnnotatedBeanDefinitionReader} is used to register the appropriate
	 * bean definitions.
	 * <p>Note that this method does not call {@link #createBeanDefinitionReader}
	 * since <code>AnnotatedBeanDefinitionReader</code> is not an instance of
	 * {@link BeanDefinitionReader}.
	 * @param context the context in which the configuration classes should be registered
	 * @param mergedConfig the merged configuration from which the classes should be retrieved
	 * @see AbstractGenericContextLoader#loadBeanDefinitions
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, MergedContextConfiguration mergedConfig) {
		Class<?>[] configClasses = mergedConfig.getClasses();
		if (logger.isDebugEnabled()) {
			logger.debug("Registering configuration classes: " + ObjectUtils.nullSafeToString(configClasses));
		}
		new AnnotatedBeanDefinitionReader(context).register(configClasses);
	}

	/**
	 * <code>AnnotationConfigContextLoader</code> should be used as a
	 * {@link org.springframework.test.context.SmartContextLoader SmartContextLoader},
	 * not as a legacy {@link org.springframework.test.context.ContextLoader ContextLoader}.
	 * Consequently, this method is not supported.
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
