/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.core;

/**
 * Interface defining a generic contract for attaching and accessing metadata
 * to/from arbitrary objects.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public interface AttributeAccessor {

	/**
	 * Set the attribute defined by <code>name</code> to the supplied	<code>value</code>.
	 * If <code>value</code> is <code>null</code>, the attribute is {@link #removeAttribute removed}.
	 * <p>In general, users should take care to prevent overlaps with other
	 * metadata attributes by using fully-qualified names, perhaps using
	 * class or package names as prefix.
	 * @param name the unique attribute key
	 * @param value the attribute value to be attached
	 */
	void setAttribute(String name, Object value);

	/**
	 * Get the value of the attribute identified by <code>name</code>.
	 * Return <code>null</code> if the attribute doesn't exist.
	 * @param name the unique attribute key
	 * @return the current value of the attribute, if any
	 */
	Object getAttribute(String name);

	/**
	 * Remove the attribute identified by <code>name</code> and return its value.
	 * Return <code>null</code> if no attribute under <code>name</code> is found.
	 * @param name the unique attribute key
	 * @return the last value of the attribute, if any
	 */
	Object removeAttribute(String name);

	/**
	 * Return <code>true</code> if the attribute identified by <code>name</code> exists.
	 * Otherwise return <code>false</code>.
	 * @param name the unique attribute key
	 */
	boolean hasAttribute(String name);

	/**
	 * Return the names of all attributes.
	 */
	String[] attributeNames();

}
