/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.tests.sample.beans;

/**
 * @author Juergen Hoeller
 * @since 17.08.2004
 */
public class BeanWithObjectProperty {

	private Object object;

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

}
