/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.core.env.Environment;

public abstract class LookupComponent {

	private Environment environment;

	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	@Lookup
	public abstract Integer getCounter();

	public Integer getCounter(Integer ignored) {
		return 0;
	}

}
