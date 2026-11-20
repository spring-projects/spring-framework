/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.support;

/**
 * @author Juergen Hoeller
 * @since 29.07.2004
 */
class ProtectedBaseBean {

	private String someProperty;

	public void setSomeProperty(String someProperty) {
		this.someProperty = someProperty;
	}

	public String getSomeProperty() {
		return someProperty;
	}

}
