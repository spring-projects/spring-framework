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

package org.springframework.beans.factory.parsing;

import org.springframework.util.StringUtils;

/**
 * {@link ParseState} entry representing a JavaBean property.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class PropertyEntry implements ParseState.Entry {

	private final String name;


	/**
	 * Creates a new instance of the {@link PropertyEntry} class.
	 * @param name the name of the JavaBean property represented by this instance
	 * @throws IllegalArgumentException if the supplied <code>name</code> is <code>null</code>
	 * or consists wholly of whitespace
	 */
	public PropertyEntry(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Invalid property name '" + name + "'.");
		}
		this.name = name;
	}


	@Override
	public String toString() {
		return "Property '" + this.name + "'";
	}

}
