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
package org.springframework.mapping.support;

import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.expression.EvaluationContext;

/**
 * Encapsulates mapping context for a type of object.
 * @param <T> the object type
 * @author Keith Donald
 */
interface MappableType<T> {

	/**
	 * The fields of the object that are eligible for mapping, including any nested fields.
	 */
	Set<String> getFields(T object);

	/**
	 * A evaluation context for accessing the object.
	 */
	EvaluationContext getEvaluationContext(T object, ConversionService conversionService);

	/**
	 * Is this object to map an instanceof this mappable type?
	 * @param object the object to test
	 */
	boolean isInstance(Object object);

}