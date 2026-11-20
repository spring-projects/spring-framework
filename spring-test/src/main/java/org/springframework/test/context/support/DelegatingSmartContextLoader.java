/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.springframework.beans.BeanUtils;
import org.springframework.test.context.SmartContextLoader;
import org.springframework.util.ClassUtils;

/**
 * {@code DelegatingSmartContextLoader} is a concrete implementation of
 * {@link AbstractDelegatingSmartContextLoader} that delegates to a
 * {@link GenericXmlContextLoader} (or a {@link GenericGroovyXmlContextLoader} if Groovy
 * is present in the classpath) and an {@link AnnotationConfigContextLoader}.
 *
 * @author Sam Brannen
 * @since 3.1
 * @see SmartContextLoader
 * @see AbstractDelegatingSmartContextLoader
 * @see GenericXmlContextLoader
 * @see GenericGroovyXmlContextLoader
 * @see AnnotationConfigContextLoader
 */
public class DelegatingSmartContextLoader extends AbstractDelegatingSmartContextLoader {

	private static final String GROOVY_XML_CONTEXT_LOADER_CLASS_NAME =
			"org.springframework.test.context.support.GenericGroovyXmlContextLoader";

	private static final boolean GROOVY_PRESENT = ClassUtils.isPresent("groovy.lang.Closure",
			DelegatingSmartContextLoader.class.getClassLoader()) &&
				ClassUtils.isPresent(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
						DelegatingSmartContextLoader.class.getClassLoader());


	private final SmartContextLoader xmlLoader;
	private final SmartContextLoader annotationConfigLoader;


	public DelegatingSmartContextLoader() {
		if (GROOVY_PRESENT) {
			try {
				Class<?> loaderClass = ClassUtils.forName(GROOVY_XML_CONTEXT_LOADER_CLASS_NAME,
					DelegatingSmartContextLoader.class.getClassLoader());
				this.xmlLoader = (SmartContextLoader) BeanUtils.instantiateClass(loaderClass);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Failed to enable support for Groovy scripts; " +
						"could not load class: " + GROOVY_XML_CONTEXT_LOADER_CLASS_NAME, ex);
			}
		}
		else {
			this.xmlLoader = new GenericXmlContextLoader();
		}

		this.annotationConfigLoader = new AnnotationConfigContextLoader();
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
