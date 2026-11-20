/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.aspectj.AspectInstanceFactory;

/**
 * Subinterface of {@link org.springframework.aop.aspectj.AspectInstanceFactory}
 * that returns {@link AspectMetadata} associated with AspectJ-annotated classes.
 *
 * @author Rod Johnson
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjType
 */
public interface MetadataAwareAspectInstanceFactory extends AspectInstanceFactory {

	/**
	 * Get the AspectJ AspectMetadata for this factory's aspect.
	 * @return the aspect metadata
	 */
	AspectMetadata getAspectMetadata();

	/**
	 * Get the best possible creation mutex for this factory.
	 * @return the mutex object (may be {@code null} for no mutex to use)
	 * @since 4.3
	 */
	@Nullable Object getAspectCreationMutex();

}
