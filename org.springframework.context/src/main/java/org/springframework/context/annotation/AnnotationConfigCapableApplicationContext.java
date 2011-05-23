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

package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Extension of the {@link ConfigurableApplicationContext} interface to be implemented by
 * application contexts that are capable of registering or scanning for annotated classes
 * including @{@link Configuration} classes.
 *
 * <p>This subinterface is not intended for everyday use:
 * {@link AnnotationConfigApplicationContext} and its web variant
 * {@code AnnotationConfigWebApplicationContext} should be used directly in most cases.
 *
 * <p>The notable exception to the above is when designing
 * {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} (ACI) implementations: it may be desirable to design an
 * ACI such that it may be used interchangeably against a standalone or web-capable
 * "AnnotationConfig" application context. For example:
 * <pre class="code">
 * public class MyACI
 *     implements ApplicationContextInitializer&lt;AnnotationConfigCapableApplicationContext&gt; {
 *   void initialize(AnnotationConfigCapableApplicationContext context) {
 *     context.register(MyConfig1.class, MyConfig2.class);
 *     context.scan("pkg1", "pkg2");
 *     // ...
 *   }
 * }</pre>
 *
 * See {@link org.springframework.context.ApplicationContextInitializer
 * ApplicationContextInitializer} Javadoc for further usage details.
 *
 * @author Chris Beams
 * @since 3.1
 * @see AnnotationConfigApplicationContext
 * @see org.springframework.web.context.support.AnnotationConfigWebApplicationContext
 * @see org.springframework.context.ApplicationContextInitializer
 */
public interface AnnotationConfigCapableApplicationContext extends ConfigurableApplicationContext {

	/**
	 * Set the {@link ScopeMetadataResolver} to use for detected bean classes.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 */
	void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver);

	/**
	 * Set the {@link BeanNameGenerator} to use for detected bean classes.
	 * <p>The default is an {@link AnnotationBeanNameGenerator}.
	 */
	void setBeanNameGenerator(BeanNameGenerator beanNameGenerator);

	/**
	 * Register one or more annotated classes to be processed.
	 * Note that {@link #refresh()} must be called in order for the context to fully
	 * process the new class.
	 * <p>Calls to {@link #register} are idempotent; adding the same
	 * annotated class more than once has no additional effect.
	 * @param annotatedClasses one or more annotated classes,
	 * e.g. {@link Configuration @Configuration} classes
	 * @see #scan(String...)
	 * @see #refresh()
	 */
	void register(Class<?>... annotatedClasses);

	/**
	 * Perform a scan within the specified base packages.
	 * Note that {@link #refresh()} must be called in order for the context to
	 * fully process the new class.
	 * @param basePackages the packages to check for annotated classes
	 * @see #register(Class...)
	 * @see #refresh()
	 */
	void scan(String... basePackages);

}
