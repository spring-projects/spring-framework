/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.springframework.beans.factory.BeanRegistrar;

/**
 * Common interface for annotation config application contexts,
 * defining {@link #register} and {@link #scan} methods.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public interface AnnotationConfigRegistry {

	/**
	 * Invoke the given registrars for registering their beans with this
	 * application context.
	 * <p>This can be used to register custom beans without inferring
	 * annotation-based characteristics for primary/fallback/lazy-init,
	 * rather specifying those programmatically if needed.
	 * @param registrars one or more {@link BeanRegistrar} instances
	 * @since 7.0
	 * @see #register(Class[])
	 */
	void register(BeanRegistrar... registrars);

	/**
	 * Register one or more component classes to be processed, inferring
	 * annotation-based characteristics for primary/fallback/lazy-init
	 * just like for scanned component classes.
	 * <p>Calls to {@code register} are idempotent; adding the same
	 * component class more than once has no additional effect.
	 * @param componentClasses one or more component classes,
	 * for example, {@link Configuration @Configuration} classes
	 * @see #scan(String...)
	 */
	void register(Class<?>... componentClasses);

	/**
	 * Perform a scan within the specified base packages.
	 * @param basePackages the packages to scan for component classes
	 * @see #register(Class[])
	 */
	void scan(String... basePackages);

}
