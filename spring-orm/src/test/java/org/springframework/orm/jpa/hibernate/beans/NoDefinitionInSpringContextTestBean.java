/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.orm.jpa.hibernate.beans;

public class NoDefinitionInSpringContextTestBean extends TestBean {

	@SuppressWarnings("unused")
	private NoDefinitionInSpringContextTestBean() {
		throw new AssertionError("Unexpected call to the default constructor. " +
				"Is Spring trying to instantiate this class by itself, even though it should delegate to the fallback producer?"
		);
	}

	/*
	 * Expect instantiation through a non-default constructor, just to be sure that Spring will fail if it tries to instantiate it,
	 * and will subsequently delegate to the fallback bean instance producer.
 	 */
	public NoDefinitionInSpringContextTestBean(String name, BeanSource source) {
		setName(name);
		setSource(source);
	}

}
