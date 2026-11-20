/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jmx.export.metadata;

/**
 * Base class for all JMX metadata classes.
 *
 * @author Rob Harrop
 * @since 1.2
 */
public abstract class AbstractJmxAttribute {

	private String description = "";

	private int currencyTimeLimit = -1;


	/**
	 * Set a description for this attribute.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return a description for this attribute.
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Set a currency time limit for this attribute.
	 */
	public void setCurrencyTimeLimit(int currencyTimeLimit) {
		this.currencyTimeLimit = currencyTimeLimit;
	}

	/**
	 * Return a currency time limit for this attribute.
	 */
	public int getCurrencyTimeLimit() {
		return this.currencyTimeLimit;
	}

}
