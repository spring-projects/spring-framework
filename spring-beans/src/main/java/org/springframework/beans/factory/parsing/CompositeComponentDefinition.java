/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory.parsing;

import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * {@link ComponentDefinition} implementation that holds one or more nested
 * {@link ComponentDefinition} instances, aggregating them into a named group
 * of components.
 *
 * @author Juergen Hoeller
 * @since 2.0.1
 * @see #getNestedComponents()
 */
public class CompositeComponentDefinition extends AbstractComponentDefinition {

	private final String name;

	private final Object source;

	private final List<ComponentDefinition> nestedComponents = new LinkedList<ComponentDefinition>();


	/**
	 * Create a new CompositeComponentDefinition.
	 * @param name the name of the composite component
	 * @param source the source element that defines the root of the composite component
	 */
	public CompositeComponentDefinition(String name, Object source) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.source = source;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getSource() {
		return this.source;
	}


	/**
	 * Add the given component as nested element of this composite component.
	 * @param component the nested component to add
	 */
	public void addNestedComponent(ComponentDefinition component) {
		Assert.notNull(component, "ComponentDefinition must not be null");
		this.nestedComponents.add(component);
	}

	/**
	 * Return the nested components that this composite component holds.
	 * @return the array of nested components, or an empty array if none
	 */
	public ComponentDefinition[] getNestedComponents() {
		return this.nestedComponents.toArray(new ComponentDefinition[this.nestedComponents.size()]);
	}

}
