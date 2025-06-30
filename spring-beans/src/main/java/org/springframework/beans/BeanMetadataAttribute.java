/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Holder for a key-value style attribute that is part of a bean definition.
 *
 * <p>Keeps track of the definition source in addition to the key-value pair.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class BeanMetadataAttribute implements BeanMetadataElement {

	private final String name;

	private final @Nullable Object value;

	private @Nullable Object source;


	/**
	 * Create a new {@code AttributeValue} instance.
	 * @param name the name of the attribute (never {@code null})
	 * @param value the value of the attribute (possibly before type conversion)
	 */
	public BeanMetadataAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.value = value;
	}


	/**
	 * Return the name of the attribute.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Return the value of the attribute.
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Set the configuration source {@code Object} for this metadata element.
	 * <p>The exact type of the object will depend on the configuration mechanism used.
	 */
	public void setSource(@Nullable Object source) {
		this.source = source;
	}

	@Override
	public @Nullable Object getSource() {
		return this.source;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other ||(other instanceof BeanMetadataAttribute that &&
				this.name.equals(that.name) &&
				ObjectUtils.nullSafeEquals(this.value, that.value) &&
				ObjectUtils.nullSafeEquals(this.source, that.source)));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(this.name, this.value);
	}

	@Override
	public String toString() {
		return "metadata attribute: name='" + this.name + "'; value=" + this.value;
	}

}
