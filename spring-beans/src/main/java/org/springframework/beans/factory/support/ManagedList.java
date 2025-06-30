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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.Mergeable;

/**
 * Tag collection class used to hold managed List elements, which may
 * include runtime bean references (to be resolved into bean objects).
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 27.05.2003
 * @param <E> the element type
 */
@SuppressWarnings("serial")
public class ManagedList<E> extends ArrayList<E> implements Mergeable, BeanMetadataElement {

	private @Nullable Object source;

	private @Nullable String elementTypeName;

	private boolean mergeEnabled;


	public ManagedList() {
	}

	public ManagedList(int initialCapacity) {
		super(initialCapacity);
	}


	/**
	 * Create a new instance containing an arbitrary number of elements.
	 * @param elements the elements to be contained in the list
	 * @param <E> the {@code List}'s element type
	 * @return a {@code ManagedList} containing the specified elements
	 * @since 5.3.16
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public static <E> ManagedList<E> of(E... elements) {
		ManagedList<E> list = new ManagedList<>();
		Collections.addAll(list, elements);
		return list;
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

	/**
	 * Set the default element type name (class name) to be used for this list.
	 */
	public void setElementTypeName(String elementTypeName) {
		this.elementTypeName = elementTypeName;
	}

	/**
	 * Return the default element type name (class name) to be used for this list.
	 */
	public @Nullable String getElementTypeName() {
		return this.elementTypeName;
	}

	/**
	 * Set whether merging should be enabled for this collection,
	 * in case of a 'parent' collection value being present.
	 */
	public void setMergeEnabled(boolean mergeEnabled) {
		this.mergeEnabled = mergeEnabled;
	}

	@Override
	public boolean isMergeEnabled() {
		return this.mergeEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<E> merge(@Nullable Object parent) {
		if (!this.mergeEnabled) {
			throw new IllegalStateException("Not allowed to merge when the 'mergeEnabled' property is set to 'false'");
		}
		if (parent == null) {
			return this;
		}
		if (!(parent instanceof List)) {
			throw new IllegalArgumentException("Cannot merge with object of type [" + parent.getClass() + "]");
		}
		List<E> merged = new ManagedList<>();
		merged.addAll((List<E>) parent);
		merged.addAll(this);
		return merged;
	}

}
