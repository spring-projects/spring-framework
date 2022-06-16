/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.generator.config;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Resolved attributes of an injected element.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class InjectedElementAttributes {

	@Nullable
	private final List<Object> attributes;


	InjectedElementAttributes(@Nullable List<Object> attributes) {
		this.attributes = attributes;
	}


	/**
	 * Specify if the attributes have been resolved.
	 * @return the resolution of the injection
	 */
	public boolean isResolved() {
		return (this.attributes != null);
	}

	/**
	 * Run the specified {@linkplain Runnable task} only if this instance is
	 * {@link #isResolved() resolved}.
	 * @param task the task to invoke if attributes are available
	 */
	public void ifResolved(Runnable task) {
		if (isResolved()) {
			task.run();
		}
	}

	/**
	 * Invoke the specified {@link Consumer} with the resolved attributes.
	 * @param attributes the consumer to invoke if this instance is resolved
	 */
	public void ifResolved(BeanDefinitionRegistrar.ThrowableConsumer<InjectedElementAttributes> attributes) {
		ifResolved(() -> attributes.accept(this));
	}

	/**
	 * Return the resolved attribute at the specified index.
	 * @param index the attribute index
	 * @param <T> the type of the attribute
	 * @return the attribute
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(int index) {
		Assert.notNull(this.attributes, "Attributes must not be null");
		return (T) this.attributes.get(index);
	}

	/**
	 * Return the resolved attribute at the specified index.
	 * @param index the attribute index
	 * @param type the attribute type
	 * @param <T> the type of the attribute
	 * @return the attribute
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(int index, Class<T> type) {
		Assert.notNull(this.attributes, "Attributes must not be null");
		return (T) this.attributes.get(index);
	}
}
