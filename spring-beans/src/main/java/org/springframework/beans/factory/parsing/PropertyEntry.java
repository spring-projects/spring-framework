/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	 * Create a new {@code PropertyEntry} instance.
	 * @param name the name of the JavaBean property represented by this instance
	 */
	public PropertyEntry(String name) {
		if (!StringUtils.hasText(name)) {
			throw new IllegalArgumentException("Invalid property name '" + name + "'");
		}
		this.name = name;
	}


	@Override
	public String toString() {
		return "Property '" + this.name + "'";
	}

}
