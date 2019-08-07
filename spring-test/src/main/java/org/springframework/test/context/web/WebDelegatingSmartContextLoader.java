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

package org.springframework.test.context.web;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.test.context.support.AbstractDelegatingSmartContextLoader;
import org.springframework.util.ClassUtils;

/**
 * {@code WebDelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlWebContextLoader} (or a {@link GenericGroovyXmlWebContextLoader} if
 * Groovy is present on the classpath) and an {@link AnnotationConfigWebContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlWebContextLoader
 * @see AnnotationConfigWebContextLoader
 */
public class WebDelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

	private static final String GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME = "org.springframework.test.context.web.GenericGroovyXmlWebContextLoader";

	private static final boolean groovyPresent = ClassUtils.isPresent("groovy.lang.Closure",
		WebDelegatingSmartContextLoader.class.getClassLoader())
			&& ClassUtils.isPresent(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
				WebDelegatingSmartContextLoader.class.getClassLoader());

	private final SmartContextLoader xmlLoader;
	private final SmartContextLoader annotationConfigLoader;


	public WebDelegatingSmartContextLoader() {
		if (groovyPresent) {
			try {
				Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME,
					WebDelegatingSmartContextLoader.class.getClassLoader());
				this.xmlLoader = (SmartContextLoader) BeanUtils.instantiateClass(loaderClass);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to enable support for Groovy scripts; "
						+ "could not load class: " + GROOVY_XML_WEB_CONTEXT_LOADER_CLASS_NAME, ex);
			}
		}
		else {
			this.xmlLoader = new GenericXmlWebContextLoader();
		}

		this.annotationConfigLoader = new AnnotationConfigWebContextLoader();
	}

	@Override
	protected SmartContextLoader getXmlLoader() {
		return this.xmlLoader;
	}

	@Override
	protected SmartContextLoader getAnnotationConfigLoader() {
		return this.annotationConfigLoader;
	}

}
