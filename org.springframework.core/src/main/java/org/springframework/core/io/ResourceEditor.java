/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.io;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.springframework.core.env.DefaultEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


/**
 * {@link java.beans.PropertyEditor Editor} for {@link Resource}
 * descriptors, to automatically convert {@code String} locations
 * e.g. {@code file:C:/myfile.txt} or {@code classpath:myfile.txt} to
 * {@code Resource} properties instead of using a {@code String} location property.
 *
 * <p>The path may contain <code>${...}</code> placeholders, to be
 * resolved as {@link Environment} properties: e.g. <code>${user.dir}</code>.
 * Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourceLoader} to do the heavy lifting,
 * by default using a {@link DefaultResourceLoader}.
 *
 * @author Juergen Hoeller
 * @author Dave Syer
 * @author Chris Beams
 * @since 28.12.2003
 * @see Resource
 * @see ResourceLoader
 * @see DefaultResourceLoader
 * @see Environment#resolvePlaceholders
 */
public class ResourceEditor extends PropertyEditorSupport {

	private final Environment environment;

	private final ResourceLoader resourceLoader;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using a {@link DefaultResourceLoader} and {@link DefaultEnvironment}.
	 */
	public ResourceEditor() {
		this(new DefaultResourceLoader(), new DefaultEnvironment());
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader} and a {@link DefaultEnvironment}.
	 * @param resourceLoader the <code>ResourceLoader</code> to use
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link #ResourceEditor(ResourceLoader, Environment)}
	 */
	@Deprecated
	public ResourceEditor(ResourceLoader resourceLoader) {
		this(resourceLoader, new DefaultEnvironment(), true);
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader} and {@link Environment}.
	 * @param resourceLoader the <code>ResourceLoader</code> to use
	 */
	public ResourceEditor(ResourceLoader resourceLoader, Environment environment) {
		this(resourceLoader, environment, true);
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader} and a {@link DefaultEnvironment}.
	 * @param resourceLoader the <code>ResourceLoader</code> to use
	 * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
	 * if no corresponding property could be found in the DefaultEnvironment
	 * @deprecated as of Spring 3.1 in favor of
	 * {@link #ResourceEditor(ResourceLoader, Environment, boolean)}
	 */
	@Deprecated
	public ResourceEditor(ResourceLoader resourceLoader, boolean ignoreUnresolvablePlaceholders) {
		this(resourceLoader, new DefaultEnvironment(), ignoreUnresolvablePlaceholders);
	}

	/**
	 * Create a new instance of the {@link ResourceEditor} class
	 * using the given {@link ResourceLoader}.
	 * @param resourceLoader the <code>ResourceLoader</code> to use
	 * @param environment the <code>Environment</code> to use
	 * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
	 * if no corresponding property could be found in the given <code>environment</code>
	 */
	public ResourceEditor(ResourceLoader resourceLoader, Environment environment, boolean ignoreUnresolvablePlaceholders) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		Assert.notNull(environment, "Environment must not be null");
		this.resourceLoader = resourceLoader;
		this.environment = environment;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			String locationToUse = resolvePath(text).trim();
			setValue(this.resourceLoader.getResource(locationToUse));
		}
		else {
			setValue(null);
		}
	}

	/**
	 * Resolve the given path, replacing placeholders with corresponding
	 * property values from the <code>environment</code> if necessary.
	 * @param path the original file path
	 * @return the resolved file path
	 * @see Environment#resolvePlaceholders
	 * @see Environment#resolveRequiredPlaceholders
	 */
	protected String resolvePath(String path) {
		PropertyResolver resolver = environment.getPropertyResolver();
		return this.ignoreUnresolvablePlaceholders ?
				resolver.resolvePlaceholders(path) :
				resolver.resolveRequiredPlaceholders(path);
	}


	@Override
	public String getAsText() {
		Resource value = (Resource) getValue();
		try {
			// Try to determine URL for resource.
			return (value != null ? value.getURL().toExternalForm() : "");
		}
		catch (IOException ex) {
			// Couldn't determine resource URL - return null to indicate
			// that there is no appropriate text representation.
			return null;
		}
	}

}
