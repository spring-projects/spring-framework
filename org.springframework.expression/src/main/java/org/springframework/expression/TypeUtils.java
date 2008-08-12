/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression;

/**
 * TypeUtilities brings together the various kinds of type related function that may occur
 * whilst working with expressions.  An implementor is providing support for four type related
 * facilities:
 * <ul>
 * <li>a mechanism for finding types
 * <li>a mechanism for comparing types
 * <li>a mechanism for type conversion/coercion
 * <li>a mechanism for overloading mathematical operations (add/subtract/etc)
 * </ul>
 * 
 * @author Andy Clement
 */
public interface TypeUtils {

	/**
	 * @return a type locator that can be used to find types, either by short or fully qualified name.
	 */
	TypeLocator getTypeLocator();

	/**
	 * @return a type comparator for comparing pairs of objects for equality.
	 */
	TypeComparator getTypeComparator();

	/**
	 * @return a type converter that can convert (or coerce) a value from one type to another.
	 */
	TypeConverter getTypeConverter();

	/**
	 * @return an operator overloader that may support mathematical operations between more than the standard set of
	 * types
	 */
	OperatorOverloader getOperatorOverloader();

}
