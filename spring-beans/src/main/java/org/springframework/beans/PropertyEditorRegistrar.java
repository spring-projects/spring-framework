/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

/**
 * Interface for strategies that register custom
 * {@link java.beans.PropertyEditor property editors} with a
 * {@link org.springframework.beans.PropertyEditorRegistry property editor registry}.
 *
 * <p>This is particularly useful when you need to use the same set of
 * property editors in several situations: write a corresponding
 * registrar and reuse that in each case.
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 */
public interface PropertyEditorRegistrar {

	/**
	 * Register custom {@link java.beans.PropertyEditor PropertyEditors} with
	 * the given {@code PropertyEditorRegistry}.
	 * <p>The passed-in registry will usually be a {@link BeanWrapper} or a
	 * {@link org.springframework.validation.DataBinder DataBinder}.
	 * <p>It is expected that implementations will create brand new
	 * {@code PropertyEditors} instances for each invocation of this
	 * method (since {@code PropertyEditors} are not threadsafe).
	 * @param registry the {@code PropertyEditorRegistry} to register the
	 * custom {@code PropertyEditors} with
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);

	/**
	 * Indicate whether this registrar exclusively overrides default editors
	 * rather than registering custom editors, intended to be applied lazily.
	 * <p>This has an impact on registrar handling in a bean factory: see
	 * {@link org.springframework.beans.factory.config.ConfigurableBeanFactory#addPropertyEditorRegistrar}.
	 * @since 6.2.3
	 * @see PropertyEditorRegistry#registerCustomEditor
	 * @see PropertyEditorRegistrySupport#overrideDefaultEditor
	 * @see PropertyEditorRegistrySupport#setDefaultEditorRegistrar
	 */
	default boolean overridesDefaultEditors() {
		return false;
	}

}
