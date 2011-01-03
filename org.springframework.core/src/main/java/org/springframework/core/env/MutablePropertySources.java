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

package org.springframework.core.env;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;


public class MutablePropertySources implements PropertySources {

	private final LinkedList<PropertySource<?>> propertySourceList = new LinkedList<PropertySource<?>>();

	static final String NON_EXISTENT_PROPERTY_SOURCE_MESSAGE = "PropertySource named [%s] does not exist";
	static final String ILLEGAL_RELATIVE_ADDITION_MESSAGE = "PropertySource named [%s] cannot be added relative to itself";


	public MutablePropertySources() {
	}

	public MutablePropertySources(PropertySources propertySources) {
		this.addAll(propertySources);
	}

	public void addAll(PropertySources propertySources) {
		for (PropertySource<?> propertySource : propertySources.asList()) {
			this.addLast(propertySource);
		}
	}

	public void addFirst(PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.addFirst(propertySource);
	}

	public void addLast(PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.addLast(propertySource);
	}

	public void addBefore(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		removeIfPresent(propertySource);
		int index = assertPresentAndGetIndex(relativePropertySourceName);
		addAtIndex(index, propertySource);
	}

	public void addAfter(String relativePropertySourceName, PropertySource<?> propertySource) {
		assertLegalRelativeAddition(relativePropertySourceName, propertySource);
		removeIfPresent(propertySource);
		int index = assertPresentAndGetIndex(relativePropertySourceName);
		addAtIndex(index+1, propertySource);
	}

	protected void assertLegalRelativeAddition(String relativePropertySourceName, PropertySource<?> propertySource) {
		String newPropertySourceName = propertySource.getName();
		Assert.isTrue(!relativePropertySourceName.equals(newPropertySourceName),
				String.format(ILLEGAL_RELATIVE_ADDITION_MESSAGE, newPropertySourceName));
	}

	protected void addAtIndex(int index, PropertySource<?> propertySource) {
		removeIfPresent(propertySource);
		this.propertySourceList.add(index, propertySource);
	}

	protected void removeIfPresent(PropertySource<?> propertySource) {
		if (this.propertySourceList.contains(propertySource)) {
			this.propertySourceList.remove(propertySource);
		}
	}

	public boolean contains(String propertySourceName) {
		return propertySourceList.contains(PropertySource.named(propertySourceName));
	}

	public PropertySource<?> remove(String propertySourceName) {
		int index = propertySourceList.indexOf(PropertySource.named(propertySourceName));
		if (index >= 0) {
			return propertySourceList.remove(index);
		}
		return null;
	}

	public void replace(String propertySourceName, PropertySource<?> propertySource) {
		int index = assertPresentAndGetIndex(propertySourceName);
		this.propertySourceList.set(index, propertySource);
	}

	protected int assertPresentAndGetIndex(String propertySourceName) {
		int index = this.propertySourceList.indexOf(PropertySource.named(propertySourceName));
		Assert.isTrue(index >= 0, String.format(NON_EXISTENT_PROPERTY_SOURCE_MESSAGE, propertySourceName));
		return index;
	}

	public int size() {
		return propertySourceList.size();
	}

	public List<PropertySource<?>> asList() {
		return Collections.unmodifiableList(this.propertySourceList);
	}

	public PropertySource<?> get(String propertySourceName) {
		return propertySourceList.get(propertySourceList.indexOf(PropertySource.named(propertySourceName)));
	}

}
