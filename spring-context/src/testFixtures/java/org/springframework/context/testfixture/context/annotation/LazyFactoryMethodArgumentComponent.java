/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.context.annotation;

import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

public class LazyFactoryMethodArgumentComponent {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	LazyFactoryMethodArgumentComponent(Environment environment, ResourceLoader resourceLoader) {
		this.environment = environment;
		this.resourceLoader = resourceLoader;
	}

	public static LazyFactoryMethodArgumentComponent of(@Lazy Environment environment, ResourceLoader resourceLoader) {
		return new LazyFactoryMethodArgumentComponent(environment, resourceLoader);
	}

	public Environment getEnvironment() {
		return this.environment;
	}

	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

}
