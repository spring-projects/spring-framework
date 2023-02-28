/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.aspectj.AspectInstanceFactory;
import org.springframework.lang.Nullable;

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
	@Nullable
	Object getAspectCreationMutex();

}
