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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextLoader;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Concrete implementation of {@link AbstractGenericContextLoader} which
 * creates an {@link AnnotationConfigApplicationContext} and registers
 * bean definitions from
 * {@link org.springframework.context.annotation.Configuration configuration classes}.
 * 
 * <p>This <code>ContextLoader</code> supports class-based context configuration
 * {@link #getResourceType() resources} as opposed to string-based resources.
 * Consequently, <em>locations</em> (as discussed in the {@link ContextLoader}
 * API and superclasses) are interpreted as fully qualified class names
 * in the context of this class. The documentation and method parameters
 * reflect this.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class AnnotationConfigContextLoader extends AbstractGenericContextLoader {

	private static final Log logger = LogFactory.getLog(AnnotationConfigContextLoader.class);


	/**
	 * Creates a new {@link AnnotationConfigApplicationContext}.
	 */
	@Override
	protected GenericApplicationContext createGenericApplicationContext() {
		return new AnnotationConfigApplicationContext();
	}

	/**
	 * Registers {@link org.springframework.context.annotation.Configuration configuration classes}
	 * in the supplied {@link AnnotationConfigApplicationContext} from the specified
	 * class names.
	 * 
	 * <p>Each class name must be the <em>fully qualified class name</em> of an
	 * annotated configuration class, component, or feature specification. The
	 * <code>AnnotationConfigApplicationContext</code> assumes the responsibility
	 * of loading the appropriate bean definitions.
	 * 
	 * <p>Note that this method does not call {@link #createBeanDefinitionReader}.
	 * @param context the context in which the configuration classes should be registered
	 * @param classNames the names of configuration classes to register in the context
	 * @throws IllegalArgumentException if the supplied context is not an instance of
	 * <code>AnnotationConfigApplicationContext</code> or if a supplied class name
	 * does not represent a class
	 * @see #createGenericApplicationContext()
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, String... classNames) {

		Assert.isInstanceOf(AnnotationConfigApplicationContext.class, context,
			"context must be an instance of AnnotationConfigApplicationContext");

		Class<?>[] configClasses = new Class<?>[classNames.length];

		for (int i = 0; i < classNames.length; i++) {
			String className = classNames[i];
			try {
				configClasses[i] = (Class<?>) getClass().getClassLoader().loadClass(className);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(String.format(
					"The supplied class name [%s] does not represent a class.", className), e);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Registering configuration classes: " + ObjectUtils.nullSafeToString(configClasses));
		}
		((AnnotationConfigApplicationContext) context).register(configClasses);
	}

	/**
	 * Returns <code>null</code>; intended as a <em>no-op</em> operation.
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return null;
	}

	/**
	 * Generates the default {@link org.springframework.context.annotation.Configuration configuration class}
	 * names array based on the supplied class.
	 * 
	 * <p>For example, if the supplied class is <code>com.example.MyTest</code>,
	 * the generated array will contain a single string with a value of
	 * &quot;com.example.MyTest<code>&lt;suffix&gt;</code>&quot;,
	 * where <code>&lt;suffix&gt;</code> is the value of the
	 * {@link #getResourceSuffix() resource suffix} string.
	 * @param clazz the class for which the default configuration class names are to be generated
	 * @return an array of default configuration class names
	 * @see #getResourceSuffix()
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String suffix = getResourceSuffix();
		Assert.hasText(suffix, "Resource suffix must not be empty");
		return new String[] { clazz.getName() + suffix };
	}

	/**
	 * Returns the supplied class names unmodified.
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... classNames) {
		return classNames;
	}

	/**
	 * Returns &quot;Config</code>&quot;; intended to be used as a suffix
	 * to append to the name of the test class when generating default
	 * configuration class names.
	 * @see #generateDefaultLocations(Class)
	 */
	@Override
	protected String getResourceSuffix() {
		return "Config";
	}

	/**
	 * Returns {@link ResourceType#CLASSES}.
	 */
	@Override
	public final ResourceType getResourceType() {
		return ResourceType.CLASSES;
	}

}
