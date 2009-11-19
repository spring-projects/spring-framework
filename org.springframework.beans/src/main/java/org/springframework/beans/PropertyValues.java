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

package org.springframework.beans;

/**
 * Holder containing one or more {@link PropertyValue} objects,
 * typically comprising one update for a specific target bean.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 13 May 2001
 * @see PropertyValue
 */
public interface PropertyValues {
   
	/** 
	 * Return an array of the PropertyValue objects held in this object.
	 */
	PropertyValue[] getPropertyValues();	
	
	/**
	 * Return the property value with the given name, if any.
	 * @param propertyName the name to search for
	 * @return the property value, or <code>null</code>
	 */
	PropertyValue getPropertyValue(String propertyName);

	/**
	 * Return the changes since the previous PropertyValues.
	 * Subclasses should also override <code>equals</code>.
	 * @param old old property values
	 * @return PropertyValues updated or new properties.
	 * Return empty PropertyValues if there are no changes.
	 * @see java.lang.Object#equals
	 */
	PropertyValues changesSince(PropertyValues old);

	/**
	 * Is there a property value (or other processing entry) for this property?
	 * @param propertyName the name of the property we're interested in
	 * @return whether there is a property value for this property
	 */
	boolean contains(String propertyName);

	/**
	 * Does this holder not contain any PropertyValue objects at all?
	 */
	boolean isEmpty();

}
