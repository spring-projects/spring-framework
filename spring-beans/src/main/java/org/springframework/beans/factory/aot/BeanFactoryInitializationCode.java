/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodReference;
import org.springframework.javapoet.ClassName;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform bean factory initialization.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 * @since 6.0
 * @see BeanFactoryInitializationAotContribution
 */
public interface BeanFactoryInitializationCode {

	/**
	 * The recommended variable name to use to refer to the bean factory.
	 */
	String BEAN_FACTORY_VARIABLE = "beanFactory";

	/**
	 * Get the {@link GeneratedMethods} used by the initializing code.
	 * @return the generated methods
	 */
	GeneratedMethods getMethods();

	/**
	 * Return the name of the class used by the initializing code.
	 * @return the generated class name
	 * @since 7.0.2
	 */
	ClassName getClassName();

	/**
	 * Add an initializer method call. An initializer can use a flexible signature,
	 * using any of the following:
	 * <ul>
	 * <li>{@code DefaultListableBeanFactory}, or {@code ConfigurableListableBeanFactory}
	 * to use the bean factory.</li>
	 * <li>{@code ConfigurableEnvironment} or {@code Environment} to access the
	 * environment.</li>
	 * <li>{@code ResourceLoader} to load resources.</li>
	 * </ul>
	 * @param methodReference a reference to the initialize method to call.
	 */
	void addInitializer(MethodReference methodReference);

}
