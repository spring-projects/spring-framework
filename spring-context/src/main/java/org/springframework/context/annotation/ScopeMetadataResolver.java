/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Strategy interface for resolving the scope of bean definitions.
 * 
 * @author Mark Fisher
 * @since 2.5
 * @see org.springframework.context.annotation.Scope
 */
public interface ScopeMetadataResolver {

	/**
	 * Resolve the {@link ScopeMetadata} appropriate to the supplied
	 * bean <code>definition</code>.
	 * <p>Implementations can of course use any strategy they like to
	 * determine the scope metadata, but some implementations that spring
	 * immediately to mind might be to use source level annotations
	 * present on {@link BeanDefinition#getBeanClassName() the class} of the
	 * supplied <code>definition</code>, or to use metadata present in the
	 * {@link BeanDefinition#attributeNames()} of the supplied <code>definition</code>.
	 * @param definition the target bean definition
	 * @return the relevant scope metadata; never <code>null</code>
	 */
	ScopeMetadata resolveScopeMetadata(BeanDefinition definition);
	
}
