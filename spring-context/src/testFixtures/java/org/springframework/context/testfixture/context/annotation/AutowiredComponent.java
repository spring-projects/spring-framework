/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class AutowiredComponent {

	private Environment environment;

	private Integer counter;

	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	@Autowired
	public void setCounter(Integer counter) {
		this.counter = counter;
	}

	public Integer getCounter() {
		return this.counter;
	}

	public Integer getCounter(Integer ignored) {
		return this.counter;
	}

}
