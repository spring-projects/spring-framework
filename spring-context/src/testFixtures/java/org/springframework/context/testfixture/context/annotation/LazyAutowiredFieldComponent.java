/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class LazyAutowiredFieldComponent {

	@Lazy
	@Autowired
	private Environment environment;

	@Autowired
	private ResourceLoader resourceLoader;

	public Environment getEnvironment() {
		return this.environment;
	}


	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}
}
