/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.io.AbstractResource;
import org.springframework.util.Assert;

/**
 * Descriptive {@link org.springframework.core.io.Resource} wrapper for
 * a {@link org.springframework.beans.factory.config.BeanDefinition}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.core.io.DescriptiveResource
 */
class BeanDefinitionResource extends AbstractResource {

	private final BeanDefinition beanDefinition;


	/**
	 * Create a new BeanDefinitionResource.
	 * @param beanDefinition the BeanDefinition objectto wrap
	 */
	public BeanDefinitionResource(BeanDefinition beanDefinition) {
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		this.beanDefinition = beanDefinition;
	}

	/**
	 * Return the wrapped BeanDefinition object.
	 */
	public final BeanDefinition getBeanDefinition() {
		return this.beanDefinition;
	}


	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isReadable() {
		return false;
	}

	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(
				"Resource cannot be opened because it points to " + getDescription());
	}

	public String getDescription() {
		return "BeanDefinition defined in " + this.beanDefinition.getResourceDescription();
	}


	/**
	 * This implementation compares the underlying BeanDefinition.
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj == this ||
		    (obj instanceof BeanDefinitionResource &&
						((BeanDefinitionResource) obj).beanDefinition.equals(this.beanDefinition)));
	}

	/**
	 * This implementation returns the hash code of the underlying BeanDefinition.
	 */
	@Override
	public int hashCode() {
		return this.beanDefinition.hashCode();
	}

}
