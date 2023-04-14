/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.type;

import org.springframework.lang.Nullable;

/**
 * Interface that defines abstract bean metadata for a specific class.
 *
 * @author Danny Thomas
 * @since 6.x
 */
public interface BeanClassMetadata {

	/**
	 * Determine if this class is a bean factory. Where a bean factory implements one of the following interfaces:
	 * <p>
	 * {@link org.springframework.beans.factory.FactoryBean}
	 * {@link org.springframework.beans.factory.ObjectFactory}
	 * @return true if the class implements a bean factory interface
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 */
	boolean isBeanFactory();

	/**
	 * If this class is a bean factory, as defined by {@link #isBeanFactory()}, return the type
	 * that the factory returns.
	 * @return the {@link TypeMetadata} for the given type. Null if this class is not a bean factory
	 * @throws ClassMetadataNotFoundException if class metadata for the class hierarchy of
	 * this class could not be loaded
	 * @see #isBeanFactory()
	 */
	@Nullable
	TypeMetadata getBeanFactoryTypeMetadata();

}
