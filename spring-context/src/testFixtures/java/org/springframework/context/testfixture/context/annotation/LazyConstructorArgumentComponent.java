/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 *
 * @author Stephane Nicoll
 */
public class LazyConstructorArgumentComponent {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	public LazyConstructorArgumentComponent(@Lazy Environment environment, ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

}
