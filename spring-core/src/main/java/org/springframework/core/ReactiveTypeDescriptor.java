/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core;

import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Describes the semantics of a reactive type including boolean checks for
 * {@link #isMultiValue()}, {@link #supportsEmpty()}, and {@link #isNoValue()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveTypeDescriptor {

	private final Class<?> reactiveType;

	@Nullable
	private final Supplier<?> emptyValueSupplier;

	private final boolean multiValue;

	private final boolean noValue;


	/**
	 * Private constructor. See static factory methods.
	 */
	private ReactiveTypeDescriptor(Class<?> reactiveType, @Nullable Supplier<?> emptySupplier,
			boolean multiValue, boolean noValue) {

		Assert.notNull(reactiveType, "'reactiveType' must not be null");
		this.reactiveType = reactiveType;
		this.emptyValueSupplier = emptySupplier;
		this.multiValue = multiValue;
		this.noValue = noValue;
	}


	/**
	 * Return the reactive type for this descriptor.
	 */
	public Class<?> getReactiveType() {
		return this.reactiveType;
	}

	/**
	 * Return {@code true} if the reactive type can produce more than 1 value
	 * can be produced and is therefore a good fit to adapt to {@code Flux}.
	 * A {@code false} return value implies the reactive type can produce 1
	 * value at most and is therefore a good fit to adapt to {@code Mono}.
	 */
	public boolean isMultiValue() {
		return this.multiValue;
	}

	/**
	 * Return {@code true} if the reactive type can complete with no values.
	 */
	public boolean supportsEmpty() {
		return (this.emptyValueSupplier != null);
	}

	/**
	 * Return {@code true} if the reactive type does not produce any values and
	 * only provides completion and error signals.
	 */
	public boolean isNoValue() {
		return this.noValue;
	}

	/**
	 * Return an empty-value instance for the underlying reactive or async type.
	 * Use of this type implies {@link #supportsEmpty()} is true.
	 */
	public Object getEmptyValue() {
		Assert.state(this.emptyValueSupplier != null, "Empty values not supported");
		return this.emptyValueSupplier.get();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return this.reactiveType.equals(((ReactiveTypeDescriptor) other).reactiveType);
	}

	@Override
	public int hashCode() {
		return this.reactiveType.hashCode();
	}


	/**
	 * Descriptor for a reactive type that can produce 0..N values.
	 * @param type the reactive type
	 * @param emptySupplier a supplier of an empty-value instance of the reactive type
	 */
	public static ReactiveTypeDescriptor multiValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, emptySupplier, true, false);
	}

	/**
	 * Descriptor for a reactive type that can produce 0..1 values.
	 * @param type the reactive type
	 * @param emptySupplier a supplier of an empty-value instance of the reactive type
	 */
	public static ReactiveTypeDescriptor singleOptionalValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, emptySupplier, false, false);
	}

	/**
	 * Descriptor for a reactive type that must produce 1 value to complete.
	 * @param type the reactive type
	 */
	public static ReactiveTypeDescriptor singleRequiredValue(Class<?> type) {
		return new ReactiveTypeDescriptor(type, null, false, false);
	}

	/**
	 * Descriptor for a reactive type that does not produce any values.
	 * @param type the reactive type
	 * @param emptySupplier a supplier of an empty-value instance of the reactive type
	 */
	public static ReactiveTypeDescriptor noValue(Class<?> type, Supplier<?> emptySupplier) {
		return new ReactiveTypeDescriptor(type, emptySupplier, false, true);
	}

}
