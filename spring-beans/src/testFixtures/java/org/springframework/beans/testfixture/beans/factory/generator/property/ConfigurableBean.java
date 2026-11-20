/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.property;

public class ConfigurableBean {

	@SuppressWarnings("unused")
	private String name;

	@SuppressWarnings("unused")
	private Integer counter;

	public void setName(String name) {
		this.name = name;
	}

	public void setCounter(Integer counter) {
		this.counter = counter;
	}

}
