/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.config.annotation;

import org.springframework.web.servlet.view.velocity.VelocityConfigurer;
import org.springframework.web.servlet.view.velocity.VelocityViewResolver;

/**
 * Encapsulates information required to create a
 * {@link org.springframework.web.servlet.view.velocity.VelocityViewResolver} and a
 * {@link org.springframework.web.servlet.view.velocity.VelocityConfigurer beans}.
 * Default configuration is "" prefix, ".vm" suffix and  "/WEB-INF/" resourceLoaderPath.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class VelocityRegistration extends ViewResolutionRegistration<VelocityViewResolver> {

	private final VelocityConfigurer configurer;

	public VelocityRegistration(ViewResolutionRegistry registry) {
		super(registry, new VelocityViewResolver());
		this.configurer = new VelocityConfigurer();
		this.prefix("");
		this.suffix(".vm");
		this.resourceLoaderPath("/WEB-INF/");
	}

	/**
	 * Set the Velocity resource loader path via a Spring resource location.
	 *
	 * @see org.springframework.web.servlet.view.velocity.VelocityConfigurer#setResourceLoaderPath(String)
	 */
	public VelocityRegistration resourceLoaderPath(String resourceLoaderPath) {
		this.configurer.setResourceLoaderPath(resourceLoaderPath);
		return this;
	}

	/**
	 * Set the prefix that gets prepended to view names when building a URL.
	 *
	 * @see org.springframework.web.servlet.view.velocity.VelocityViewResolver#setPrefix(String)
	 */
	public VelocityRegistration prefix(String prefix) {
		this.viewResolver.setPrefix(prefix);
		return this;
	}

	/**
	 * Set the suffix that gets appended to view names when building a URL.
	 *
	 * @see org.springframework.web.servlet.view.velocity.VelocityViewResolver#setSuffix(String)
	 */
	public VelocityRegistration suffix(String suffix) {
		this.viewResolver.setSuffix(suffix);
		return this;
	}

	/**
	 * Enable or disable caching.
	 *
	 * @see org.springframework.web.servlet.view.velocity.VelocityViewResolver#setCache(boolean)
	 */
	public VelocityRegistration cache(boolean cache) {
		this.viewResolver.setCache(cache);
		return this;
	}

	protected VelocityConfigurer getConfigurer() {
		return this.configurer;
	}
}
