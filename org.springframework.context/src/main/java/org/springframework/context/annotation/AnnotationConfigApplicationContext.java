/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Standalone application context, accepting annotated classes as input - in particular
 * {@link org.springframework.context.annotation.Configuration @Configuration}-annotated
 * classes, but also plain {@link org.springframework.stereotype.Component @Components}
 * and JSR-330 compliant classes using {@literal javax.inject} annotations. Allows for
 * registering classes one by one ({@link #register}) as well as for classpath scanning
 * ({@link #scan}).
 *
 * <p>In case of multiple Configuration classes, {@link Bean} methods defined in later
 * classes will override those defined in earlier classes. This can be leveraged to
 * deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 * @see #register
 * @see #scan
 * @see AnnotatedBeanDefinitionReader
 * @see ClassPathBeanDefinitionScanner
 * @see org.springframework.context.support.GenericXmlApplicationContext
 */
public class AnnotationConfigApplicationContext extends GenericApplicationContext {

	private final AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(this);

	private final ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this);


	/**
 	 * Create a new AnnotationConfigApplicationContext that needs to be populated
	 * through {@link #register} calls and then manually {@link #refresh refreshed}.
	 */
	public AnnotationConfigApplicationContext() {
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions
	 * from the given annotated classes and automatically refreshing the context.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 */
	public AnnotationConfigApplicationContext(Class<?>... annotatedClasses) {
		register(annotatedClasses);
		refresh();
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for bean definitions
	 * in the given packages and automatically refreshing the context.
	 * @param basePackages the packages to check for annotated classes
	 */
	public AnnotationConfigApplicationContext(String... basePackages) {
		scan(basePackages);
		refresh();
	}


	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>Default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.reader.setBeanNameGenerator(beanNameGenerator);
		this.scanner.setBeanNameGenerator(beanNameGenerator);
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.reader.setScopeMetadataResolver(scopeMetadataResolver);
		this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
	}


	/**
	 * Register an annotated class to be processed. Allows for programmatically
	 * building a {@link AnnotationConfigApplicationContext}. Note that
	 * {@link AnnotationConfigApplicationContext#refresh()} must be called in
	 * order for the context to fully process the new class.
	 * <p>Calls to {@link #register} are idempotent; adding the same
	 * annotated class more than once has no additional effect.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 * @see #refresh()
	 */
	public void register(Class<?>... annotatedClasses) {
		this.reader.register(annotatedClasses);
	}

	/**
	 * Perform a scan within the specified base packages.
	 * Note that {@link AnnotationConfigApplicationContext#refresh()} must be
	 * called in order for the context to fully process the new class.
	 * @param basePackages the packages to check for annotated classes
	 * @see #refresh()
	 */
	public void scan(String... basePackages) {
		this.scanner.scan(basePackages);
	}

}
