/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.io.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.Resource;

/**
 * Editor for {@link org.springframework.core.io.Resource} arrays, to
 * automatically convert <code>String</code> location patterns
 * (e.g. <code>"file:C:/my*.txt"</code> or <code>"classpath*:myfile.txt"</code>)
 * to <code>Resource</code> array properties. Can also translate a collection
 * or array of location patterns into a merged Resource array.
 *
 * <p>A path may contain <code>${...}</code> placeholders, to be
 * resolved as {@link org.springframework.core.env.Environment} properties:
 * e.g. <code>${user.dir}</code>. Unresolvable placeholders are ignored by default.
 *
 * <p>Delegates to a {@link ResourcePatternResolver},
 * by default using a {@link PathMatchingResourcePatternResolver}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 1.1.2
 * @see org.springframework.core.io.Resource
 * @see ResourcePatternResolver
 * @see PathMatchingResourcePatternResolver
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

	private static final Log logger = LogFactory.getLog(ResourceArrayPropertyEditor.class);

	private final PropertyResolver propertyResolver;

	private final ResourcePatternResolver resourcePatternResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * Create a new ResourceArrayPropertyEditor with a default
	 * {@link PathMatchingResourcePatternResolver} and {@link StandardEnvironment}.
	 * @see PathMatchingResourcePatternResolver
	 * @see Environment
	 */
	public ResourceArrayPropertyEditor() {
		this(new PathMatchingResourcePatternResolver(), new StandardEnvironment(), true);
	}

	/**
	 * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
	 * and a {@link StandardEnvironment}.
	 * @param resourcePatternResolver the ResourcePatternResolver to use
	 * @deprecated as of 3.1 in favor of {@link #ResourceArrayPropertyEditor(ResourcePatternResolver, PropertyResolver)}
	 */
	@Deprecated
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver) {
		this(resourcePatternResolver, new StandardEnvironment(), true);
	}

	/**
	 * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
	 * and {@link PropertyResolver} (typically an {@link Environment}).
	 * @param resourcePatternResolver the ResourcePatternResolver to use
	 * @param propertyResolver the PropertyResolver to use
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver, PropertyResolver propertyResolver) {
		this(resourcePatternResolver, propertyResolver, true);
	}

	/**
	 * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
	 * and a {@link StandardEnvironment}.
	 * @param resourcePatternResolver the ResourcePatternResolver to use
	 * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
	 * if no corresponding system property could be found
	 * @deprecated as of 3.1 in favor of {@link #ResourceArrayPropertyEditor(ResourcePatternResolver, PropertyResolver, boolean)}
	 */
	@Deprecated
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver, boolean ignoreUnresolvablePlaceholders) {
		this(resourcePatternResolver, new StandardEnvironment(), ignoreUnresolvablePlaceholders);
	}

	/**
	 * Create a new ResourceArrayPropertyEditor with the given {@link ResourcePatternResolver}
	 * and {@link PropertyResolver} (typically an {@link Environment}).
	 * @param resourcePatternResolver the ResourcePatternResolver to use
	 * @param propertyResolver the PropertyResolver to use
	 * @param ignoreUnresolvablePlaceholders whether to ignore unresolvable placeholders
	 * if no corresponding system property could be found
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
			PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {
		this.resourcePatternResolver = resourcePatternResolver;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	/**
	 * Treat the given text as a location pattern and convert it to a Resource array.
	 */
	@Override
	public void setAsText(String text) {
		String pattern = resolvePath(text).trim();
		try {
			setValue(this.resourcePatternResolver.getResources(pattern));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
		}
	}

	/**
	 * Treat the given value as a collection or array and convert it to a Resource array.
	 * Considers String elements as location patterns and takes Resource elements as-is.
	 */
	@Override
	public void setValue(Object value) throws IllegalArgumentException {
		if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
			Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
			List<Resource> merged = new ArrayList<Resource>();
			for (Object element : input) {
				if (element instanceof String) {
					// A location pattern: resolve it into a Resource array.
					// Might point to a single resource or to multiple resources.
					String pattern = resolvePath((String) element).trim();
					try {
						Resource[] resources = this.resourcePatternResolver.getResources(pattern);
						for (Resource resource : resources) {
							if (!merged.contains(resource)) {
								merged.add(resource);
							}
						}
					}
					catch (IOException ex) {
						// ignore - might be an unresolved placeholder or non-existing base directory
						if (logger.isDebugEnabled()) {
							logger.debug("Could not retrieve resources for pattern '" + pattern + "'", ex);
						}
					}
				}
				else if (element instanceof Resource) {
					// A Resource object: add it to the result.
					Resource resource = (Resource) element;
					if (!merged.contains(resource)) {
						merged.add(resource);
					}
				}
				else {
					throw new IllegalArgumentException("Cannot convert element [" + element + "] to [" +
							Resource.class.getName() + "]: only location String and Resource object supported");
				}
			}
			super.setValue(merged.toArray(new Resource[merged.size()]));
		}

		else {
			// An arbitrary value: probably a String or a Resource array.
			// setAsText will be called for a String; a Resource array will be used as-is.
			super.setValue(value);
		}
	}

	/**
	 * Resolve the given path, replacing placeholders with
	 * corresponding system property values if necessary.
	 * @param path the original file path
	 * @return the resolved file path
	 * @see PropertyResolver#resolvePlaceholders
	 * @see PropertyResolver#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		return this.ignoreUnresolvablePlaceholders ?
				this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path);
	}

}
