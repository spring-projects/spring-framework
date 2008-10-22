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

package org.springframework.core.type;

import java.util.Map;
import java.util.Set;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @since 2.5
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 */
public interface AnnotationMetadata extends ClassMetadata {

	/**
	 * Return the names of all annotation types defined on the underlying class.
	 * @return the annotation type names
	 */
	Set<String> getAnnotationTypes();

	/**
	 * Determine whether the underlying class has an annotation of the given
	 * type defined.
	 * @param annotationType the annotation type to look for
	 * @return whether a matching annotation is defined
	 */
	boolean hasAnnotation(String annotationType);

	/**
	 * Return the names of all meta-annotation types defined on the
	 * given annotation type of the underlying class.
	 * @return the meta-annotation type names
	 */
	Set<String> getMetaAnnotationTypes(String annotationType);

	/**
	 * Determine whether the underlying class has an annotation that
	 * is itself annotated with the meta-annotation of the given type.
	 * @param metaAnnotationType the meta-annotation type to look for
	 * @return whether a matching meta-annotation is defined
	 */
	boolean hasMetaAnnotation(String metaAnnotationType);

	/**
	 * Retrieve the attributes of the annotation of the given type,
	 * if any (i.e. if defined on the underlying class).
	 * @param annotationType the annotation type to look for
	 * @return a Map of attributes, with the attribute name as key
	 * (e.g. "value") and the defined attribute value as Map value.
	 * This return value will be <code>null</code> if no matching
	 * annotation is defined.
	 */
	Map<String, Object> getAnnotationAttributes(String annotationType);

}
