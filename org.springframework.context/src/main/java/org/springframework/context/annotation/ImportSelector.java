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

/**
 * Interface to be implemented by types that determine which @{@link Configuration}
 * class(es) should be imported based on a given selection criteria, usually one or more
 * annotation attributes.
 *
 * <p>In certain cases, an {@code ImportSelector} may register additional bean definitions
 * through the {@code BeanDefinitionRegistry} available in the
 * {@link ImportSelectorContext} provided to the {@link #selectImports} method.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Import
 * @see Configuration
 */
public interface ImportSelector {

	/**
	 * Select and return the names of which class(es) should be imported based on
	 * the {@code AnnotationMetadata} of the importing {@code @Configuration} class and
	 * optionally register any {@code BeanDefinition}s necessary to support the selected
	 * classes.
	 * @param context containing the {@code AnnotationMetadata} of the importing @{@link
	 * Configuration} class and the enclosing {@code BeanDefinitionRegistry}.
	 */
	String[] selectImports(ImportSelectorContext context);

}
