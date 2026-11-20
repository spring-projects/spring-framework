/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.test;

/**
 * @author Juergen Hoeller
 */
public class ExtendedPerson extends ConcretePerson {

	private Object someField;


	public Object getSomeField() {
		return someField;
	}

	public void setSomeField(Object someField) {
		this.someField = someField;
	}

}
