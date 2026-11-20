/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory;

/**
 * Callback interface for initializing a Spring {@link ListableBeanFactory}
 * prior to entering the singleton pre-instantiation phase. Can be used to
 * trigger early initialization of specific beans before regular singletons.
 *
 * <p>Can be programmatically applied to a {@code ListableBeanFactory} instance.
 * In an {@code ApplicationContext}, beans of type {@code BeanFactoryInitializer}
 * will be autodetected and automatically applied to the underlying bean factory.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @param <F> the bean factory type
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface BeanFactoryInitializer<F extends ListableBeanFactory> {

	/**
	 * Initialize the given bean factory.
	 * @param beanFactory the bean factory to bootstrap
	 */
	void initialize(F beanFactory);

}
