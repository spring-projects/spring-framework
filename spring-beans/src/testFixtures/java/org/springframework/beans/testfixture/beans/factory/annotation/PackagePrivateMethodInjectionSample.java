/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class PackagePrivateMethodInjectionSample {

	public Environment environment;

	@Autowired
	void setTestBean(Environment environment) {
		this.environment = environment;
	}

}
