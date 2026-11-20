/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

/**
 * Metadata about JMX operation parameters.
 * Used in conjunction with a {@link ManagedOperation} attribute.
 *
 * @author Rob Harrop
 * @since 1.2
 */
public class ManagedOperationParameter {

	private int index = 0;

	private String name = "";

	private String description = "";


	/**
	 * Set the index of this parameter in the operation signature.
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Return the index of this parameter in the operation signature.
	 */
	public int getIndex() {
		return this.index;
	}

	/**
	 * Set the name of this parameter in the operation signature.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of this parameter in the operation signature.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Set a description for this parameter.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return a description for this parameter.
	 */
	public String getDescription() {
		return this.description;
	}

}
