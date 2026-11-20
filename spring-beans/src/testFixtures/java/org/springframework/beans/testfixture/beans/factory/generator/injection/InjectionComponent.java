/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.injection;

import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("unused")
public class InjectionComponent {

	private final String bean;

	private Integer counter;

	public InjectionComponent(String bean) {
		this.bean = bean;
	}

	@Autowired(required = false)
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

}
