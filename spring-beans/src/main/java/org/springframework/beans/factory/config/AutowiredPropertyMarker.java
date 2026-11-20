/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Simple marker class for an individually autowired property value, to be added
 * to {@link BeanDefinition#getPropertyValues()} for a specific bean property.
 *
 * <p>At runtime, this will be replaced with a {@link DependencyDescriptor}
 * for the corresponding bean property's write method, eventually to be resolved
 * through a {@link AutowireCapableBeanFactory#resolveDependency} step.
 *
 * @author Juergen Hoeller
 * @since 5.2
 * @see AutowireCapableBeanFactory#resolveDependency
 * @see BeanDefinition#getPropertyValues()
 * @see org.springframework.beans.factory.support.BeanDefinitionBuilder#addAutowiredProperty
 */
@SuppressWarnings("serial")
public final class AutowiredPropertyMarker implements Serializable {

	/**
	 * The canonical instance for the autowired marker value.
	 */
	public static final Object INSTANCE = new AutowiredPropertyMarker();


	private AutowiredPropertyMarker() {
	}

	private Object readResolve() {
		return INSTANCE;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other);
	}

	@Override
	public int hashCode() {
		return AutowiredPropertyMarker.class.hashCode();
	}

	@Override
	public String toString() {
		return "(autowired)";
	}

}
