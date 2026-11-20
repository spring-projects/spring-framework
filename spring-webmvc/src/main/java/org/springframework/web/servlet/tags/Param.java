/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags;

import org.jspecify.annotations.Nullable;

/**
 * Bean used to pass name-value pair parameters from a {@link ParamTag} to a
 * {@link ParamAware} tag.
 *
 * <p>Attributes are the raw values passed to the spring:param tag and have not
 * been encoded or escaped.
 *
 * @author Scott Andrews
 * @since 3.0
 * @see ParamTag
 */
public class Param {

	private @Nullable String name;

	private @Nullable String value;


	/**
	 * Set the raw name of the parameter.
	 */
	public void setName(@Nullable String name) {
		this.name = name;
	}

	/**
	 * Return the raw parameter name.
	 */
	public @Nullable String getName() {
		return this.name;
	}

	/**
	 * Set the raw value of the parameter.
	 */
	public void setValue(@Nullable String value) {
		this.value = value;
	}

	/**
	 * Return the raw parameter value.
	 */
	public @Nullable String getValue() {
		return this.value;
	}


	@Override
	public String toString() {
		return "JSP Tag Param: name '" + this.name + "', value '" + this.value + "'";
	}

}
