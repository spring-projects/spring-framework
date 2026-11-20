/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.aot;

/**
 * A bean that uses {@link CustomPropertyValue}.
 *
 * @author Stephane Nicoll
 */
public class CustomBean {

	private CustomPropertyValue customPropertyValue;

	public CustomPropertyValue getCustomPropertyValue() {
		return this.customPropertyValue;
	}

	public void setCustomPropertyValue(CustomPropertyValue customPropertyValue) {
		this.customPropertyValue = customPropertyValue;
	}

}
