/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import jakarta.annotation.Resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class LazyResourceFieldComponent {

	@Lazy
	@Resource
	private Environment environment;

	@Resource
	private ResourceLoader resourceLoader;

	public Environment getEnvironment() {
		return this.environment;
	}

	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

}
