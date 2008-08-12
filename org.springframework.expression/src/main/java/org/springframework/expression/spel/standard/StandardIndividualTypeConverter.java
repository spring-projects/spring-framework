/*
 * Copyright 2004-2007 the original author or authors.
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
package org.springframework.expression.spel.standard;

import org.springframework.expression.EvaluationException;

/**
 * Implementations of this interface are able to convert from some set of types to another type.  For
 * example they might be able to convert some set of number types (Integer.class, Double.class) to
 * a string (String.class).  Once created they are registered with the {@link StandardEvaluationContext} or
 * {@link StandardTypeConverter}.
 * 
 * @author Andy Clement
 */
public interface StandardIndividualTypeConverter {

	/**
	 * @return return the set of classes which this converter can convert from.
	 */
	Class<?>[] getFrom();

	/**
	 * @return the class which this converter can convert to.
	 */
	Class<?> getTo();

	/**
	 * Return a value converted to the type that {@link #getTo()} specified.
	 * 
	 * @param value the object to convert
	 * @return the converted value
	 * @throws EvaluationException if there is a problem during conversion
	 */
	Object convert(Object value) throws EvaluationException;
}