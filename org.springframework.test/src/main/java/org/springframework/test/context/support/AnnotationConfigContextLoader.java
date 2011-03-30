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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * TODO Document AnnotationConfigContextLoader.
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
	 * TODO Document overridden loadBeanDefinitions().
	 */
	@Override
	protected void loadBeanDefinitions(GenericApplicationContext context, String... locations) {

		Assert.isInstanceOf(AnnotationConfigApplicationContext.class, context,
			"context must be an instance of AnnotationConfigApplicationContext");

		AnnotationConfigApplicationContext annotationConfigApplicationContext = (AnnotationConfigApplicationContext) context;

		List<Class<?>> configClasses = new ArrayList<Class<?>>();
		for (String location : locations) {
			try {
				Class<?> clazz = getClass().getClassLoader().loadClass(location);
				configClasses.add(clazz);
			}
			catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(String.format(
					"The supplied resource location [%s] does not represent a class.", location), e);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Registering configuration classes: " + configClasses);
		}

		for (Class<?> configClass : configClasses) {
			annotationConfigApplicationContext.register(configClass);
		}
	}

	/**
	 * Returns <code>null</code>; intended as a <em>no-op</em> operation.
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(GenericApplicationContext context) {
		return null;
	}

	/**
	 * TODO Document overridden generateDefaultLocations().
	 */
	@Override
	protected String[] generateDefaultLocations(Class<?> clazz) {
		// TODO Implement generateDefaultLocations().
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Returns the supplied <code>locations</code> unmodified.
	 */
	@Override
	protected String[] modifyLocations(Class<?> clazz, String... locations) {
		return locations;
	}

	/**
	 * Returns &quot;Config</code>&quot;.
	 */
	@Override
	protected String getResourceSuffix() {
		return "Config";
	}

}
