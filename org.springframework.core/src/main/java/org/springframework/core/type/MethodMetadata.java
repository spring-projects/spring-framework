/*
 * Copyright 2002-2009 the original author or authors.
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
 * @author Mark Pollack
 * @since 3.0
 */
public interface MethodMetadata {

	int getModifiers();
	
	boolean isStatic();
	
	String getMethodName();
	
	//TODO does the method return type have a generic wildcard or generic type parameters?
	
	// annotation metadata
	
	Set<String> getAnnotationTypes();
	
	boolean hasAnnotation(String annotationType);
	
	Map<String, Object> getAnnotationAttributes(String annotationType);
	
	/**
	 * Determine whether the underlying class has an annotation that
	 * is itself annotated with the meta-annotation of the given type.
	 * @param metaAnnotationType the meta-annotation type to look for
	 * @return whether a matching meta-annotation is defined
	 */
	boolean hasMetaAnnotation(String metaAnnotationType);
	
	/**
	 * Return the names of all meta-annotation types defined on the
	 * given annotation type of the underlying class.
	 * @return the meta-annotation type names
	 */
	Set<String> getMetaAnnotationTypes(String annotationType);

	Set<String> getAnnotationTypesWithMetaAnnotation(String qualifierClassName);
}
