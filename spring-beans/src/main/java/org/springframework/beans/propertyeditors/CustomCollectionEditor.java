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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Property editor for Collections, converting any source Collection
 * to a given target Collection type.
 *
 * <p>By default registered for Set, SortedSet and List,
 * to automatically convert any given Collection to one of those
 * target types if the type does not match the target property.
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see java.util.Collection
 * @see java.util.Set
 * @see java.util.SortedSet
 * @see java.util.List
 */
public class CustomCollectionEditor extends PropertyEditorSupport {

	@SuppressWarnings("rawtypes")
	private final Class<? extends Collection> collectionType;

	private final boolean nullAsEmptyCollection;


	/**
	 * Create a new CustomCollectionEditor for the given target type,
	 * keeping an incoming {@code null} as-is.
	 * @param collectionType the target type, which needs to be a
	 * sub-interface of Collection or a concrete Collection class
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType) {
		this(collectionType, false);
	}

	/**
	 * Create a new CustomCollectionEditor for the given target type.
	 * <p>If the incoming value is of the given type, it will be used as-is.
	 * If it is a different Collection type or an array, it will be converted
	 * to a default implementation of the given Collection type.
	 * If the value is anything else, a target Collection with that single
	 * value will be created.
	 * <p>The default Collection implementations are: ArrayList for List,
	 * TreeSet for SortedSet, and LinkedHashSet for Set.
	 * @param collectionType the target type, which needs to be a
	 * sub-interface of Collection or a concrete Collection class
	 * @param nullAsEmptyCollection whether to convert an incoming {@code null}
	 * value to an empty Collection (of the appropriate type)
	 * @see java.util.Collection
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("rawtypes")
	public CustomCollectionEditor(Class<? extends Collection> collectionType, boolean nullAsEmptyCollection) {
		Assert.notNull(collectionType, "Collection type is required");
		if (!Collection.class.isAssignableFrom(collectionType)) {
			throw new IllegalArgumentException(
					"Collection type [" + collectionType.getName() + "] does not implement [java.util.Collection]");
		}
		this.collectionType = collectionType;
		this.nullAsEmptyCollection = nullAsEmptyCollection;
	}


	/**
	 * Convert the given text value to a Collection with a single element.
	 */
	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(text);
	}

	/**
	 * Convert the given value to a Collection of the target type.
	 */
	@Override
	public void setValue(@Nullable Object value) {
		if (value == null && this.nullAsEmptyCollection) {
			super.setValue(createCollection(this.collectionType, 0));
		}
		else if (value == null || (this.collectionType.isInstance(value) && !alwaysCreateNewCollection())) {
			// Use the source value as-is, as it matches the target type.
			super.setValue(value);
		}
		else if (value instanceof Collection<?> source) {
			// Convert Collection elements.
			Collection<Object> target = createCollection(this.collectionType, source.size());
			for (Object elem : source) {
				target.add(convertElement(elem));
			}
			super.setValue(target);
		}
		else if (value.getClass().isArray()) {
			// Convert array elements to Collection elements.
			int length = Array.getLength(value);
			Collection<Object> target = createCollection(this.collectionType, length);
			for (int i = 0; i < length; i++) {
				target.add(convertElement(Array.get(value, i)));
			}
			super.setValue(target);
		}
		else {
			// A plain value: convert it to a Collection with a single element.
			Collection<Object> target = createCollection(this.collectionType, 1);
			target.add(convertElement(value));
			super.setValue(target);
		}
	}

	/**
	 * Create a Collection of the given type, with the given
	 * initial capacity (if supported by the Collection type).
	 * @param collectionType a sub-interface of Collection
	 * @param initialCapacity the initial capacity
	 * @return the new Collection instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected Collection<Object> createCollection(Class<? extends Collection> collectionType, int initialCapacity) {
		if (!collectionType.isInterface()) {
			try {
				return ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate collection class: " + collectionType.getName(), ex);
			}
		}
		else if (List.class == collectionType) {
			return new ArrayList<>(initialCapacity);
		}
		else if (SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else {
			return new LinkedHashSet<>(initialCapacity);
		}
	}

	/**
	 * Return whether to always create a new Collection,
	 * even if the type of the passed-in Collection already matches.
	 * <p>Default is "false"; can be overridden to enforce creation of a
	 * new Collection, for example to convert elements in any case.
	 * @see #convertElement
	 */
	protected boolean alwaysCreateNewCollection() {
		return false;
	}

	/**
	 * Hook to convert each encountered Collection/array element.
	 * The default implementation simply returns the passed-in element as-is.
	 * <p>Can be overridden to perform conversion of certain elements,
	 * for example String to Integer if a String array comes in and
	 * should be converted to a Set of Integer objects.
	 * <p>Only called if actually creating a new Collection!
	 * This is by default not the case if the type of the passed-in Collection
	 * already matches. Override {@link #alwaysCreateNewCollection()} to
	 * enforce creating a new Collection in every case.
	 * @param element the source element
	 * @return the element to be used in the target Collection
	 * @see #alwaysCreateNewCollection()
	 */
	protected Object convertElement(Object element) {
		return element;
	}


	/**
	 * This implementation returns {@code null} to indicate that
	 * there is no appropriate text representation.
	 */
	@Override
	public @Nullable String getAsText() {
		return null;
	}

}
